package com.ceslab.firemesh.meshmodule.bluetoothmesh

import com.ceslab.firemesh.meshmodule.database.NodeFunctionalityDataBase
import com.ceslab.firemesh.meshmodule.listener.ConfigurationTaskListener
import com.ceslab.firemesh.meshmodule.listener.NodeFeatureListener
import com.ceslab.firemesh.meshmodule.listener.NodeRetransmissionListener
import com.ceslab.firemesh.meshmodule.model.ConfigurationTask
import com.ceslab.firemesh.meshmodule.model.MeshNode
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
                unsubscribeModelFromGroup(oldGroup, meshNodeToConfigure.functionality)
            )
            taskList.add(unbindNodeFromGroup(oldGroup))
        }
        taskList.add(updateFunctionalityInSharedPreference(NodeFunctionality.VENDOR_FUNCTIONALITY.Unknown))
        if (newGroup != null) {
            taskList.add(bindNodeToGroup(newGroup))
        }
        startTasks()
    }

    fun processChangeFunctionality(
        newFunctionality: NodeFunctionality.VENDOR_FUNCTIONALITY,
        isSetPublication: Boolean,
        isAddSubscription: Boolean
    ) {
        Timber.d("changeFunctionality: functionality=$newFunctionality -- publication=$isSetPublication --subscription=$isAddSubscription")
        if (meshNodeToConfigure.node.groups.isEmpty()) {
            meshNodeManager.removeNodeFunc(meshNodeToConfigure)
            return
        }
        val group = meshNodeToConfigure.node.groups.first()
        group?.let {
            taskList.addAll(
                startUnbindModelFromGroupAndClearPublicationSettings(
                    it,
                    meshNodeToConfigure.functionality,
                    isSetPublication,
                    isAddSubscription
                )
            )
            taskList.addAll(
                startBindModelToGroupAndSetPublicationSettings(
                    it,
                    newFunctionality,
                    isSetPublication,
                    isAddSubscription
                )
            )
        }
        taskList.add(updateFunctionalityInSharedPreference(newFunctionality))
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
            listener.onCurrentConfigTask(ConfigurationTask.BIND_NODE_TO_GROUP)
        }
        return Runnable {
            nodeControl.bind(group, NodeControlCallbackImpl())
        }
    }

    private fun unbindNodeFromGroup(group: Group): Runnable {
        Timber.d("unbindNodeToGroup")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.UNBIND_NODE_FROM_GROUP)
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
        functionality: NodeFunctionality.VENDOR_FUNCTIONALITY,
        isSetPublication: Boolean,
        isAddSubscription: Boolean
    ): List<Runnable> {
        val tasks = mutableListOf<Runnable>()
        NodeFunctionality.getVendorModels(meshNodeToConfigure.node, functionality)
            .forEach { model ->
                if (isAddSubscription) {
                    tasks.add(removeSubscriptionSettings(model, group))
                }
                if (isSetPublication) {
                    tasks.add(clearPublicationSettings(model))
                }
                tasks.add(unbindModelFromGroup(model, group))
            }
        return tasks
    }

    private fun unsubscribeModelFromGroup(
        group: Group,
        functionality: NodeFunctionality.VENDOR_FUNCTIONALITY
    ): List<Runnable> {
        val tasks = mutableListOf<Runnable>()
        NodeFunctionality.getVendorModels(
            meshNodeToConfigure.node,
            functionality
        ).forEach { model ->
            tasks.add(removeSubscriptionSettings(model, group))
        }
        return tasks
    }


    private fun bindModelToGroup(vendorModel: VendorModel, group: Group): Runnable {
        Timber.d("bindModelToGroup")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.BIND_MODEL_TO_GROUP)
        }
        return Runnable {
            val functionalityBinder = FunctionalityBinder(group)
            functionalityBinder.bindModel(vendorModel, FunctionalityBinderCallbackImpl())
        }
    }

    private fun unbindModelFromGroup(vendorModel: VendorModel, group: Group): Runnable {
        Timber.d("unbindModelFromGroup")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.UNBIND_MODEL_FROM_GROUP)
        }
        return Runnable {
            val functionalityBinder = FunctionalityBinder(group)
            functionalityBinder.unbindModel(vendorModel, FunctionalityBinderCallbackImpl())
        }
    }

    private fun setPublicationSettings(model: VendorModel, group: Group): Runnable {
        Timber.d("setPublicationSettings")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.SET_PUBLICATION_SETTING)
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
            listener.onCurrentConfigTask(ConfigurationTask.CLEAR_PUBLICATION_SETTING)
        }
        return Runnable {
            val subscriptionControl = SubscriptionControl(model)
            subscriptionControl.clearPublicationSettings(PublicationSettingsGenericCallbackImpl())
        }
    }


    private fun addSubscriptionSettings(model: VendorModel, group: Group): Runnable {
        Timber.d("addSubscriptionSettings")
        configurationTaskListeners.forEach { listener ->
            listener.onCurrentConfigTask(ConfigurationTask.ADD_SUBSCRIPTION_SETTING)
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
            listener.onCurrentConfigTask(ConfigurationTask.REMOVE_SUBSCRIPTION_SETTING)
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

    private fun updateFunctionalityInSharedPreference(functionality: NodeFunctionality.VENDOR_FUNCTIONALITY): Runnable {
        Timber.d("updateFunctionalityInSharedPreference")
        return Runnable {
            try {
                meshNodeManager.updateNodeFunc(meshNodeToConfigure, functionality)
                takeNextTask()
            } catch (e: NodeChangeNameException) {
                Timber.e(e.localizedMessage)
            }
        }
    }

    fun checkProxyStatus() {
        Timber.d("checkProxyStatus")
        configurationControl.checkProxyStatus(object : CheckNodeBehaviourCallbackImpl() {
            override fun success(node: Node?, enabled: Boolean) {
                nodeFeatureListeners.forEach { listener -> listener.onGetProxyStatusSucceed(enabled) }
                super.success(node, enabled)
            }
        })
    }

    fun checkRelayStatus() {
        Timber.d("checkRelayStatus")
        configurationControl.checkRelayStatus(object : CheckNodeBehaviourCallbackImpl() {

            override fun success(node: Node?, enabled: Boolean) {
                nodeFeatureListeners.forEach { listener -> listener.onGetRelayStatusSucceed(enabled) }
                super.success(node, enabled)
            }

        })
    }

    fun checkFriendStatus() {
        Timber.d("checkFriendStatus")
        configurationControl.checkFriendStatus(object : CheckNodeBehaviourCallbackImpl() {

            override fun success(node: Node?, enabled: Boolean) {
                nodeFeatureListeners.forEach { listener -> listener.onGetFriendStatusSucceed(enabled) }
                super.success(node, enabled)
            }
        })
    }

    fun checkRetransmissionStatus(){
        Timber.d("checkRetransmissionStatus")
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

    inner class NodeControlCallbackImpl : NodeControlCallback {
        override fun succeed() {
            Timber.d("nodeControlCallback succeed")
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
            takeNextTask()
        }

        override fun error(meshModel: Model?, errorType: ErrorType) {
            Timber.e("subscriptionSetting error: $errorType")
            configurationTaskListeners.forEach { listener ->
                listener.onConfigError(errorType)
            }
        }
    }

    abstract inner class SetNodeBehaviourCallbackImpl : SetNodeBehaviourCallback {
        override fun error(node: Node?, error: ErrorType) {
            nodeFeatureListeners.forEach { listener ->
                listener.onSetNodeFeatureError(error)
            }
        }
    }


}