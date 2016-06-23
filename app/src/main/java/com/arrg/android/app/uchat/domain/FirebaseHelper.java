package com.arrg.android.app.uchat.domain;

import com.arrg.android.app.uchat.entities.User;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    private Firebase dataReference;
    private final static String SEPARATOR = "___";
    private final static String USERS_PATH = "users";
    private final static String CONTACTS_PATH = "contacts";
    private final static String CHATS_PATH = "chats";
    private final static String FIREBASE_URL = "https://uchat-ec499.firebaseio.com";

    public static FirebaseHelper getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public FirebaseHelper() {
        this.dataReference = new Firebase(FIREBASE_URL);
    }

    public Firebase getDataReference() {
        return dataReference;
    }

    public String getAuthUserEmail() {
        AuthData authData = getDataReference().getAuth();
        String email = null;

        if (authData != null) {
            Map<String, Object> providerData = authData.getProviderData();
            email = providerData.get("email").toString();
        }

        return email;
    }

    public Firebase getUserReference(String email) {
        Firebase userReference = null;

        if (email != null) {
            String emailKey = getKey(email);
            userReference = getDataReference().getRoot().child(USERS_PATH).child(emailKey);
        }

        return userReference;
    }

    public Firebase getMyUserReference() {
        return getUserReference(getAuthUserEmail());
    }

    public Firebase getContactsReference(String email) {
        return getUserReference(email).child(CONTACTS_PATH);
    }

    public Firebase getMyContactsReference() {
        return getContactsReference(getAuthUserEmail());
    }

    public Firebase getOneContactReference(String mainEmail, String childEmail) {
        String childKey = getKey(childEmail);
        return getUserReference(mainEmail).child(CONTACTS_PATH).child(childKey);
    }

    public Firebase getChatReference(String receiver) {
        String keySender = getKey(getAuthUserEmail());
        String keyReceiver = getKey(receiver);

        String keyChat = keySender + SEPARATOR + keyReceiver;
        if (keySender.compareTo(keyReceiver) > 0) {
            keyChat = keyReceiver + SEPARATOR + keySender;
        }

        return getDataReference().getRoot().child(CHATS_PATH).child(keyChat);
    }

    public void changeUserConnectionStatus(boolean isOnline) {
        if (getMyUserReference() != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("online", isOnline);

            getMyUserReference().updateChildren(updates);

            notifyContactsOfConnectionChange(isOnline);
        }
    }

    public void notifyContactsOfConnectionChange(boolean isOnLine) {
        notifyContactsOfConnectionChange(isOnLine, false);
    }

    public void LoginOut() {
        notifyContactsOfConnectionChange(User.OFFLINE, true);
    }

    private void notifyContactsOfConnectionChange(final boolean isOnLine, final boolean isLoggingOut) {
        final String myEmail = getAuthUserEmail();
        getMyContactsReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String email = child.getKey();
                    Firebase reference = getOneContactReference(email, myEmail);
                    reference.setValue(isOnLine);
                }
                if (isLoggingOut) {
                    getDataReference().unauth();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public String getKey(String key) {
        return key.replace(".", "_");
    }

    private static class SingletonHolder {
        private static final FirebaseHelper INSTANCE = new FirebaseHelper();
    }
}
