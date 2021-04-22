package com.ceslab.firemesh.meshmodule.bluetoothmesh

import com.ceslab.firemesh.meshmodule.listener.ConfigurationTaskListener
import com.ceslab.firemesh.meshmodule.listener.NodeFeatureListener
import com.ceslab.firemesh.meshmodule.listener.NodeRetransmissionListener
import com.ceslab.firemesh.meshmodule.model.ConfigurationTask
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.configuration_control.CheckNodeBehaviourCallback
import com.siliconlab.bluetoothmesh.adk.configuration_control.ConfigurationControl
import com.siliconlab.bluetoothmesh.adk.configuration_control.NodeRetransmissionConfigurationCallback
import com.siliconlab.bluetoothmesh.adk.configuration_control.SetNodeBehaviourCallback
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import com.siliconlab.bluetoothmesh.adk.data_model.model.Model
import com.siliconlab.bluetoothmesh.adk.data_model.model.VendorModel
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node
import com.siliconlab.bluetoothmesh.adk.data_model.node.NodeChangeNameException
import com.siliconlab.bluetoothmesh.adk.functionality_binder.FunctionalityBinder
import com.siliconlab.bluetoothmesh.adk.functionality_binder.FunctionalityBinderCallback
import com.siliconlab.bluetoothmesh.adk.node_control.NodeControl
import com.siliconlab.bluetoothmesh.adk.node_control.NodeControlCallback
import com.siliconlab.bluetoothmesh.adk.notification_control.PublicationSettingsGenericCallback
import com.siliconlab.bluetoothmesh.adk.notification_control.SubscriptionControl
import com.siliconlab.bluetoothmesh.adk.notification_control.SubscriptionSettingsGenericCallback
import com.siliconlab.bluetoothmesh.adk.notification_control.settings.PublicationSettings
import com.siliconlab.bluetoothmesh.adk.notification_control.settings.SubscriptionSettings
import timber.log.Timber
import java.util.concurrent.Executors

class MeshConfigurationManager(
    private val bluetoothMeshManager: BluetoothMeshManager,
    private val meshNodeManager: MeshNodeManager
) {

    companion object {
        private const val PUBLICATION_TIME_TO_LIVE = 3
        private const val RETRANSMISSION_COUNT = 3
        private const val RETRANSMISSION_INTERVAL = 200

    }
    private val nodeFeatureListeners: ArrayList<NodeFeatureListener> = ArrayList()
    private val nodeRetransmissionListeners: ArrayList<NodeRetransmissionListener> = ArrayList()

    private val configurationTaskListeners: ArrayList<ConfigurationTaskListener> = ArrayList()

    private var meshNodeToConfigure = bluetoothMeshManager.meshNodeToConfigure!!
    private var configurationControl: ConfigurationControl =
        ConfigurationControl(meshNodeToConfigure.node)
    private var nodeControl: NodeControl = NodeControl((meshNodeToConfigure.node))
    private var taskExecutor = Executors.newSingleThreadScheduledExecutor()
    private var taskList = mutableListOf<Runnable>()
    private var taskCount = 0
    private var currentTask = Runnable { }

    fun initMeshConfiguration() {
        meshNodeToConfigure = bluetoothMeshManager.meshNodeToConfigure!!
        nodeControl = NodeControl((meshNodeToConfigure.node))
        configurationControl = ConfigurationControl(meshNodeToConfigure.node)
        taskExecutor = Executors.newSingleThreadScheduledExecutor()
        taskList = mutableListOf<Runnable>()
        taskCount = 0
        currentTask = Runnable { }
    }

    fun addNodeRetransmissionListener(nodeRetransmissionListener: NodeRetransmissionListener) {
        synchronized(nodeRetransmissionListeners) {
            nodeRetransmissionListeners.add(nodeRetransmissionListener)
        }
    }

    fun removeNodeRetransmissionListener(nodeRetransmissionListener: NodeRetransmissionListener) {
        synchronized(nodeRetransmissionListeners) {
            nodeRetransmissionListeners.remove(nodeRetransmissionListener)
        }
    }

    fun addNodeFeatureListener(nodeFeatureListener: NodeFeatureListener) {
        synchronized(nodeFeatureListeners) {
            nodeFeatureListeners.add(nodeFeatureListener)
        }
    }

    fun removeNodeFeatureListener(nodeFeatureListener: NodeFeatureListener) {
        synchronized(nodeFeatureListeners) {
            nodeFeatureListeners.remove(nodeFeatureListener)
        }
    }

    fun addConfigurationTaskListener(configurationTaskListener: ConfigurationTaskListener) {
        synchronized(configurationTaskListeners) {
            configurationTaskListeners.add(configurationTaskListener)
        }
    }

    fun removeConfigurationTaskListener(configurationTaskListener: ConfigurationTaskListener) {
        synchronized(configurationTaskListeners) {
            configurationTaskListeners.remove(configurationTaskListener)
        }
    }

    fun processChangeGroup(newGroup: Group?) {
        Timber.d("processChangeGroup: $newGroup")
        if (meshNodeToConfigure.node.groups.isNotEmpty()) {
            Timber.d("processChangeGroup: isNotEmpty")
            val oldGroup = meshNodeToConfigure.node.groups.first()
            taskList.addAll(
                unsubscribeModelFromGroup(oldGroup, meshNodeToConfigure.functionalityList)
            )
            taskList.add(unbindNodeFromGroup(oldGroup))
        }
        taskList.add(clearFunctionalityInSharedPreference())
        if (newGroup != null) {
            taskList.add(bindNodeToGroup(newGroup))
        }
        startTasks()
    }

    fun processBindModelToGroup(
        functionality: NodeFunctionality.VENDOR_FUNCTIONALITY,
        isSetPublication: Boolean,
        isAddSubscription: Boolean
    ) {
        Timber.d("processBindModelToGroup: functionality=$functionality -- publication=$isSetPublication --subscription=$isAddSubscription")
        if (meshNodeToConfigure.node.groups.isEmpty()) {
            meshNodeManager.clearNodeFunctionalityList(meshNodeToConfigure)
            return
        }
        val group = meshNodeToConfigure.node.groups.first()
        group?.let {
            taskList.addAll(
                startBindModelToGroupAndSetPublicationSettings(
                    it,
                    functionality,
                    isSetPublication,
                    isAddSubscription
                )
            )
        }
        taskList.add(addFunctionalityInSharedPreference(functionality))
        startTasks()
    }

    fun processUnbindModelFromGroup(
        functionality: NodeFunctionality.VENDOR_FUNCTIONALITY) {
        Timber.d("processUnbindModelToGroup: functionality=$functionality")
        if (meshNodeToConfigure.node.groups.isEmpty()) {
            return
        }

        val group = meshNodeToConfigure.node.groups.first()
        group?.let {
            taskList.addAll(startUnbindModelFromGroupAndClearPublicationSettings(it, functionality))
        }
        taskList.add(removeFunctionalityInSharedPreference(functionality))
        startTasks()
    }


    fun startTasks() {
        Timber.d("startTasks")
        taskCount = taskList.size
        takeNextTask()
    }

    private fun clearTasks() {
        Timber.d("clearTasks")
        taskList.clear()
    }

    private fun takeNextTask() {
        Timber.d("takeNextTask: ${taskList.isEmpty()}")
        if (taskList.isNotEmpty()) {
            currentTask = taskList.first()
            taskList.remove(currentTask)
            taskExecutor.execute(currentTask)
        } else {
            configurationTaskListeners.forEach { listener ->
                listener.onConfigFinish()
            }
        }
    }

    private fun bindNodeToGroup(group: Group): Runnable {
        Timber.d("bindNodeToGroup")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.CONFIG_BIND_NODE_TO_GROUP)
        }
        return Runnable {
            nodeControl.bind(group, NodeControlCallbackImpl())
        }
    }

    private fun unbindNodeFromGroup(group: Group): Runnable {
        Timber.d("unbindNodeToGroup")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.CONFIG_UNBIND_NODE_FROM_GROUP)
        }
        return Runnable {
            nodeControl.unbind(group, NodeControlCallbackImpl())
        }
    }

    private fun startBindModelToGroupAndSetPublicationSettings(
        group: Group,
        functionality: NodeFunctionality.VENDOR_FUNCTIONALITY,
        isSetPublication: Boolean,
        isAddSubscription: Boolean
    ): List<Runnable> {
        Timber.d("startBindModelToGroupAndSetPublicationSettings")
        val taskList = mutableListOf<Runnable>()

        NodeFunctionality.getVendorModels(
            meshNodeToConfigure.node,
            functionality
        ).forEach { model ->
            taskList.add(bindModelToGroup(model, group))
            if(isSetPublication){
                taskList.add(setPublicationSettings(model, group))
            }
            if(isAddSubscription){
                taskList.add(addSubscriptionSettings(model, group))
            }
        }
        return taskList
    }

    private fun startUnbindModelFromGroupAndClearPublicationSettings(
        group: Group,
        functionality: NodeFunctionality.VENDOR_FUNCTIONALITY
    ): List<Runnable> {
        val tasks = mutableListOf<Runnable>()
        NodeFunctionality.getVendorModels(meshNodeToConfigure.node, functionality)
            .forEach { model ->
                tasks.add(removeSubscriptionSettings(model, group))
                tasks.add(clearPublicationSettings(model))
                tasks.add(unbindModelFromGroup(model, group))
            }
        return tasks
    }

    private fun unsubscribeModelFromGroup(
        group: Group,
        functionalityList: Set<NodeFunctionality.VENDOR_FUNCTIONALITY>
    ): List<Runnable> {
        Timber.d("unsubscribeModelFromGroup: ${functionalityList}")
        val tasks = mutableListOf<Runnable>()
        for(functionality in functionalityList){
            NodeFunctionality.getVendorModels(
                meshNodeToConfigure.node,
                functionality
            ).forEach { model ->
                Timber.d("unsubscribeModelFromGroup= ${model.vendorAssignedModelIdentifier()}")
                tasks.add(removeSubscriptionSettings(model, group))
            }
        }
        return tasks
    }


    private fun bindModelToGroup(vendorModel: VendorModel, group: Group): Runnable {
        Timber.d("bindModelToGroup")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.CONFIG_BIND_MODEL_TO_GROUP)
        }
        return Runnable {
            val functionalityBinder = FunctionalityBinder(group)
            functionalityBinder.bindModel(vendorModel, FunctionalityBinderCallbackImpl())
        }
    }

    private fun unbindModelFromGroup(vendorModel: VendorModel, group: Group): Runnable {
        Timber.d("unbindModelFromGroup")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.CONFIG_UNBIND_MODEL_FROM_GROUP)
        }
        return Runnable {
            val functionalityBinder = FunctionalityBinder(group)
            functionalityBinder.unbindModel(vendorModel, FunctionalityBinderCallbackImpl())
        }
    }

    private fun setPublicationSettings(model: VendorModel, group: Group): Runnable {
        Timber.d("setPublicationSettings")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.CONFIG_PUBLICATION_SETTING)
        }
        return Runnable {
            val subscriptionControl = SubscriptionControl(model)
            val publicationSettings = PublicationSettings(group)
            publicationSettings.ttl = PUBLICATION_TIME_TO_LIVE
            subscriptionControl.setPublicationSettings(
                publicationSettings, PublicationSettingsGenericCallbackImpl()
            )
        }
    }

    private fun clearPublicationSettings(model: VendorModel): Runnable {
        Timber.d("clearPublicationSettings")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.CONFIG_PUBLICATION_CLEARING)
        }
        return Runnable {
            val subscriptionControl = SubscriptionControl(model)
            subscriptionControl.clearPublicationSettings(PublicationSettingsGenericCallbackImpl())
        }
    }


    private fun addSubscriptionSettings(model: VendorModel, group: Group): Runnable {
        Timber.d("addSubscriptionSettings")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.CONFIG_SUBSCRIPTION_ADDING)
        }
        return Runnable {
            val subscriptionControl = SubscriptionControl(model)
            val subscriptionSettings = SubscriptionSettings(group)
            subscriptionControl.addSubscriptionSettings(
                subscriptionSettings,
                SubscriptionSettingsGenericCallbackImpl()
            )
        }
    }

    private fun removeSubscriptionSettings(model: VendorModel, group: Group): Runnable {
        Timber.d("removeSubscriptionSettings")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.CONFIG_SUBSCRIPTION_REMOVING)
        }
        return Runnable {
            val subscriptionSettings = SubscriptionSettings(group)
            val subscriptionControl = SubscriptionControl(model)

            subscriptionControl.removeSubscriptionSettings(
                subscriptionSettings,
                SubscriptionSettingsGenericCallbackImpl()
            )
        }
    }

    private fun addFunctionalityInSharedPreference(functionality: NodeFunctionality.VENDOR_FUNCTIONALITY): Runnable {
        Timber.d("updateFunctionalityInSharedPreference")
        return Runnable {
            try {
                meshNodeManager.addNodeFunctionalityToList(meshNodeToConfigure, functionality)
                takeNextTask()
            } catch (e: NodeChangeNameException) {
                Timber.e(e.localizedMessage)
            }
        }
    }

    private fun removeFunctionalityInSharedPreference(functionality: NodeFunctionality.VENDOR_FUNCTIONALITY): Runnable {
        Timber.d("removeFunctionalityInSharedPreference")
        return Runnable {
            try {
                meshNodeManager.removeNodeFunctionalityFromList(meshNodeToConfigure, functionality)
                takeNextTask()
            } catch (e: NodeChangeNameException) {
                Timber.e(e.localizedMessage)
            }
        }
    }

    private fun clearFunctionalityInSharedPreference(): Runnable {
        Timber.d("clearFunctionalityInSharedPreference")
        return Runnable {
            try {
                meshNodeManager.clearNodeFunctionalityList(meshNodeToConfigure)
                takeNextTask()
            } catch (e: NodeChangeNameException) {
                Timber.e(e.localizedMessage)
            }
        }
    }

    fun checkProxyStatus() {
        Timber.d("checkProxyStatus")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.CONFIG_PROXY_CHECKING)
        }
        configurationControl.checkProxyStatus(object : CheckNodeBehaviourCallbackImpl() {
            override fun success(node: Node?, enabled: Boolean) {
                nodeFeatureListeners.forEach { listener -> listener.onGetProxyStatusSucceed(enabled) }
                super.success(node, enabled)
            }
        })
    }

    fun checkRelayStatus() {
        Timber.d("checkRelayStatus")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.CONFIG_RELAY_CHECKING)
        }
        configurationControl.checkRelayStatus(object : CheckNodeBehaviourCallbackImpl() {
            override fun success(node: Node?, enabled: Boolean) {
                nodeFeatureListeners.forEach { listener -> listener.onGetRelayStatusSucceed(enabled) }
                super.success(node, enabled)
            }

        })
    }

    fun checkFriendStatus() {
        Timber.d("checkFriendStatus")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.CONFIG_FRIEND_CHECKING)
        }

        configurationControl.checkFriendStatus(object : CheckNodeBehaviourCallbackImpl() {
            override fun success(node: Node?, enabled: Boolean) {
                nodeFeatureListeners.forEach { listener -> listener.onGetFriendStatusSucceed(enabled) }
                super.success(node, enabled)
            }
        })
    }

    fun checkRetransmissionStatus(){
        Timber.d("checkRetransmissionStatus")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.CONFIG_RETRANSMISSION_CHECKING)
        }
        configurationControl.checkRetransmissionConfigurationStatus(object :NodeRetransmissionConfigurationCallback {
            override fun success(node: Node?, retransmissionCount: Int, retransmissionIntervalSteps: Int) {
                nodeRetransmissionListeners.forEach { listener -> listener.success(retransmissionCount != 0) }
                takeNextTask()
            }

            override fun error(node: Node?, error: ErrorType?) {
                nodeRetransmissionListeners.forEach { listener -> listener.error(node,error) }
                clearTasks()
            }

        })

    }

    fun changeProxy(enabled: Boolean) {
        Timber.d("changeProxy: $enabled")
        if(enabled){
            configurationTaskListeners.forEach { listener ->
                listener.onCurrentConfigTask(ConfigurationTask.CONFIG_PROXY_ENABLING)
            }
        } else {
            configurationTaskListeners.forEach { listener ->
                listener.onCurrentConfigTask(ConfigurationTask.CONFIG_PROXY_DISABLING)
            }
        }
        configurationControl.setProxy(enabled, object : SetNodeBehaviourCallbackImpl() {
            override fun success(node: Node?, enabled: Boolean) {
                Timber.d("changeProxy success: $enabled")
                nodeFeatureListeners.forEach { listener -> listener.onGetProxyStatusSucceed(enabled) }
                takeNextTask()
            }
        })
    }

    fun changeRelay(enabled: Boolean) {
        Timber.d("changeRelay: $enabled")
        if(enabled){
            configurationTaskListeners.forEach { listener ->
                listener.onCurrentConfigTask(ConfigurationTask.CONFIG_RELAY_ENABLING)
            }
        } else {
            configurationTaskListeners.forEach { listener ->
                listener.onCurrentConfigTask(ConfigurationTask.CONFIG_RELAY_DISABLING)
            }
        }
        configurationControl.setRelay(enabled, RETRANSMISSION_COUNT, RETRANSMISSION_INTERVAL, object : SetNodeBehaviourCallbackImpl() {
            override fun success(node: Node?, enabled: Boolean) {
                Timber.d("changeRelay success: $enabled")
                nodeFeatureListeners.forEach { listener -> listener.onGetRelayStatusSucceed(enabled) }
                takeNextTask()
            }
        })
    }

    fun changeFriend(enabled: Boolean) {
        Timber.d("changeFriend: $enabled")
        if(enabled){
            configurationTaskListeners.forEach { listener ->
                listener.onCurrentConfigTask(ConfigurationTask.CONFIG_FRIEND_ENABLING)
            }
        } else {
            configurationTaskListeners.forEach { listener ->
                listener.onCurrentConfigTask(ConfigurationTask.CONFIG_FRIEND_DISABLING)
            }
        }
        configurationControl.setFriend(enabled, object : SetNodeBehaviourCallbackImpl() {
            override fun success(node: Node?, enabled: Boolean) {
                Timber.d("changeFriend success: $enabled")
                nodeFeatureListeners.forEach { listener -> listener.onGetFriendStatusSucceed(enabled) }
                takeNextTask()
            }
        })
    }

    fun changeRetransmission(enabled:Boolean) {
        Timber.d("changeRetransmission: $enabled")
        if(enabled){
            configurationTaskListeners.forEach { listener ->
                listener.onCurrentConfigTask(ConfigurationTask.CONFIG_RETRANSMISSION_ENABLING)
            }
        } else {
            configurationTaskListeners.forEach { listener ->
                listener.onCurrentConfigTask(ConfigurationTask.CONFIG_RETRANSMISSION_DISABLING)
            }
        }
        val count = if(enabled) RETRANSMISSION_COUNT else 0
        val interval = if(enabled) RETRANSMISSION_INTERVAL else 0
        configurationControl.setRetransmissionConfiguration(count,interval,object :NodeRetransmissionConfigurationCallback{
            override fun success(node: Node?, retransmissionCount: Int, retransmissionIntervalSteps: Int) {
                nodeRetransmissionListeners.forEach { listener -> listener.success(retransmissionCount != 0) }
                takeNextTask()
            }

            override fun error(node: Node?, error: ErrorType?) {
                nodeRetransmissionListeners.forEach { listener -> listener.error(node,error) }

            }
        })
    }


    abstract inner class CheckNodeBehaviourCallbackImpl : CheckNodeBehaviourCallback {
        override fun success(node: Node?, enabled: Boolean) {
            Timber.d("checkNodeBehaviorCallBack succeed: $enabled")
            takeNextTask()
        }

        override fun error(node: Node?, error: ErrorType) {
            Timber.e("checkNodeBehaviorCallBack error: $error")
            configurationTaskListeners.forEach { listener ->
                listener.onConfigError(error)
            }
            clearTasks()
        }
    }

    abstract inner class SetNodeBehaviourCallbackImpl : SetNodeBehaviourCallback {
        override fun error(node: Node?, error: ErrorType) {
            nodeFeatureListeners.forEach { listener ->
                listener.onSetNodeFeatureError(error)
            }
        }
    }


    inner class NodeControlCallbackImpl : NodeControlCallback {
        override fun succeed() {
            Timber.d("nodeControlCallback succeed")
            configurationTaskListeners.forEach { listener ->
                listener.onCurrentConfigTask(ConfigurationTask.CONFIG_CONTROL_NODE_SUCCEED)
            }
            takeNextTask()
        }

        override fun error(errorType: ErrorType) {
            Timber.e("nodeControlCallback error: $errorType")
            configurationTaskListeners.forEach { listener ->
                listener.onConfigError(errorType)
            }
        }
    }

    inner class FunctionalityBinderCallbackImpl : FunctionalityBinderCallback {
        override fun succeed(succeededModels: MutableList<Model>?, group: Group?) {
            Timber.d("modelBinder succeed")
            configurationTaskListeners.forEach { listener ->
                listener.onCurrentConfigTask(ConfigurationTask.CONFIG_CONTROL_MODEL_SUCCEED)
            }
            takeNextTask()
        }

        override fun error(
            failedModels: MutableList<Model>?,
            group: Group?,
            errorType: ErrorType
        ) {
            Timber.e("modelBinder error: $errorType")
            configurationTaskListeners.forEach { listener ->
                listener.onConfigError(errorType)
            }
        }
    }

    inner class PublicationSettingsGenericCallbackImpl : PublicationSettingsGenericCallback {
        override fun success(meshModel: Model?, publicationSettings: PublicationSettings?) {
            Timber.d("publicationSetting succeed")
            configurationTaskListeners.forEach { listener ->
                listener.onCurrentConfigTask(ConfigurationTask.CONFIG_CONTROL_PUBLICATION_SUCCEED)
            }
            takeNextTask()
        }

        override fun error(meshModel: Model?, errorType: ErrorType) {
            Timber.e("publicationSetting error: $errorType")
            configurationTaskListeners.forEach { listener ->
                listener.onConfigError(errorType)
            }
        }
    }

    inner class SubscriptionSettingsGenericCallbackImpl : SubscriptionSettingsGenericCallback {
        override fun success(meshModel: Model?, subscriptionSettings: SubscriptionSettings?) {
            Timber.d("subscriptionSetting succeed")
            configurationTaskListeners.forEach { listener ->
                listener.onCurrentConfigTask(ConfigurationTask.CONFIG_CONTROL_SUBSCRIPTION_SUCCEED)
            }
            takeNextTask()
        }

        override fun error(meshModel: Model?, errorType: ErrorType) {
            Timber.e("subscriptionSetting error: $errorType")
            configurationTaskListeners.forEach { listener ->
                listener.onConfigError(errorType)
            }
        }
    }



}