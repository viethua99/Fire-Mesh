package com.ceslab.firemesh.util;

import android.content.Context;

import cn.pedant.SweetAlert.SweetAlertDialog;


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
                .setContentText(message)
                .setContentTextSize(17)
                .setCustomImage(null)
                .show();
    }


    public void showWarningDialog(Context context,String message) {
        hideDialog();
        sweetAlertDialog = new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE);
        sweetAlertDialog
                .setContentText(message)
                .setContentTextSize(17)
                .setCustomImage(null)
                .show();
    }

    public void showFailureDialog(Context context,String message) {
        hideDialog();
        sweetAlertDialog = new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE);
        sweetAlertDialog
                .setContentText(message)
                .setContentTextSize(17)
                .setCustomImage(null)
                .show();
    }

    public void showLoadingDialog(Context context,String message) {
        hideDialog();
        sweetAlertDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        sweetAlertDialog
                .setContentText(message)
                .setContentTextSize(17)
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