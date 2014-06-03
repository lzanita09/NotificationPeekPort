package com.reindeercrafts.notificationpeek.utils;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;

import java.io.IOException;
import java.io.InputStream;

/**
 * Helper class for retrieving contact photo.
 *
 * Created by zhelu on 3/21/14.
 */
public class ContactHelper {

    /**
     * Get contact photo as Bitmap with the name of the person.
     *
     * @param context   Context object of the caller.
     * @param name      Name of the contact.
     * @return          Bitmap object of the large contact photo.
     */
    public static Bitmap getContactPhoto(Context context, String name) {
        InputStream inputStream = openDisplayPhoto(context, getContactId(name, context));

        return BitmapFactory.decodeStream(inputStream);
    }


    /**
     * Get contact ID with the given contact name.
     *
     * @param name      Name of the contact.
     * @param context   Context object of the caller.
     * @return          Contact ID.
     */
    public static long getContactId(String name, Context context) {
        long ret = -1;
        String selection =
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like'%" + name + "%'";
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID};
        Cursor c = context.getContentResolver()
                .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selection,
                        null, null);
        if (c.moveToFirst()) {
            ret = c.getLong(0);
        }
        c.close();

        return ret;
    }

    /**
     * Get the InputStream object of the contact photo with given contact ID.
     *
     * @param context       Context object of the caller.
     * @param contactId     Contact ID.
     * @return              InputStream object of the contact photo.
     */
    public static InputStream openDisplayPhoto(Context context, long contactId) {
        Uri contactUri =
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri displayPhotoUri =
                Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.DISPLAY_PHOTO);
        try {
            AssetFileDescriptor fd =
                    context.getContentResolver().openAssetFileDescriptor(displayPhotoUri, "r");
            return fd.createInputStream();
        } catch (IOException e) {
            return null;
        }
    }


}
