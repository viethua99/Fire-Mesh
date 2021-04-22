package com.ceslab.firemesh.util;

import android.content.Context;

import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by Viet Hua on 11/30/2020.
 */

public class AndroidDialogUtil {

    private AndroidDialogUtil() {

    }

    private static AndroidDialogUtil INSTANCE;
    private SweetAlertDialog sweetAlertDialog;

    public static AndroidDialogUtil getInstance() {
        AndroidDialogUtil localInstance;
        if (INSTANCE == null) {
            synchronized (AndroidDialogUtil.class) {
                if (INSTANCE == null) {
                    localInstance = new AndroidDialogUtil();
                    INSTANCE = localInstance;
                }
            }
        }
        return INSTANCE;
    }

    public void showSuccessDialog(Context context, String message) {
        hideDialog();
        sweetAlertDialog = new SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE);
        sweetAlertDialog
                .setTitleText(message)
                .setCustomImage(null)
                .show();
    }

    public void setOnConfirmClickedListener(SweetAlertDialog.OnSweetClickListener listener) {
        if (sweetAlertDialog != null) {
            sweetAlertDialog.setConfirmClickListener(listener);
        }
    }

    public void showWarningDialog(Context context,String message) {
        hideDialog();
        sweetAlertDialog = new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE);
        sweetAlertDialog
                .setTitleText(message)
                .setCustomImage(null)
                .show();
    }

    public void showFailureDialog(Context context,String message) {
        hideDialog();
        sweetAlertDialog = new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE);
        sweetAlertDialog
                .setTitleText(message)
                .setCustomImage(null)
                .show();
    }

    public void showLoadingDialog(Context context,String message) {
        hideDialog();
        sweetAlertDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        sweetAlertDialog
                .setTitleText(message)
                .setCancelable(false);
        sweetAlertDialog.show();
    }

    public void hideDialog() {
        if (sweetAlertDialog != null) {
            sweetAlertDialog.dismiss();
        }
        sweetAlertDialog = null;
    }

}