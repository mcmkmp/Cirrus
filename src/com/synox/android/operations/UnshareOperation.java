/**
 *   ownCloud Android client application
 *
 *   @author masensio
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.synox.android.operations;

import android.content.Context;

import com.synox.android.datamodel.OCFile;

import com.synox.android.lib.common.OwnCloudClient;
import com.synox.android.lib.common.operations.RemoteOperationResult;
import com.synox.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.synox.android.lib.common.utils.Log_OC;
import com.synox.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.synox.android.lib.resources.shares.OCShare;
import com.synox.android.lib.resources.shares.RemoveRemoteShareOperation;
import com.synox.android.lib.resources.shares.ShareType;

import com.synox.android.operations.common.SyncOperation;

import java.util.ArrayList;

/**
 * Unshare file/folder
 * Save the data in Database
 */
public class UnshareOperation extends SyncOperation {

    private static final String TAG = UnshareOperation.class.getSimpleName();

    private String mRemotePath;
    private ShareType mShareType;
    private String mShareWith;
    private Context mContext;

    public UnshareOperation(String remotePath, ShareType shareType, String shareWith,
                            Context context) {
        mRemotePath = remotePath;
        mShareType = shareType;
        mShareWith = shareWith;
        mContext = context;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result  = null;

        // Get Share for a file
        OCShare share = getStorageManager().getFirstShareByPathAndType(mRemotePath,
                mShareType, mShareWith);

        if (share != null) {
            RemoveRemoteShareOperation operation =
                    new RemoveRemoteShareOperation((int) share.getRemoteId());
            result = operation.execute(client);

            if (result.isSuccess() || result.getCode() == ResultCode.SHARE_NOT_FOUND) {
                Log_OC.d(TAG, "Share id = " + share.getRemoteId() + " deleted");

                OCFile file = getStorageManager().getFileByPath(mRemotePath);
                if (mShareType == ShareType.PUBLIC_LINK) {
                    file.setShareViaLink(false);
                    file.setPublicLink("");
                } else if (mShareType == ShareType.USER || mShareType == ShareType.GROUP){
                    // Check if it is the last share
                    ArrayList <OCShare> sharesWith = getStorageManager().
                            getSharesWithForAFile(mRemotePath,
                                    getStorageManager().getAccount().name);
                    if (sharesWith.size() == 1) {
                        file.setShareWithSharee(false);
                    }
                }

                getStorageManager().saveFile(file);
                getStorageManager().removeShare(share);

                if (result.getCode() == ResultCode.SHARE_NOT_FOUND) {
                    if (existsFile(client, file.getRemotePath())) {
                        result = new RemoteOperationResult(ResultCode.OK);
                    } else {
                        getStorageManager().removeFile(file, true, true);
                    }
                }
            }

        } else {
            result = new RemoteOperationResult(ResultCode.SHARE_NOT_FOUND);
        }

        return result;
    }

    private boolean existsFile(OwnCloudClient client, String remotePath){
        ExistenceCheckRemoteOperation existsOperation =
                new ExistenceCheckRemoteOperation(remotePath, mContext, false);
        RemoteOperationResult result = existsOperation.execute(client);
        return result.isSuccess();
    }

}