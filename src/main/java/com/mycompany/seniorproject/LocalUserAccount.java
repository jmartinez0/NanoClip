package com.mycompany.seniorproject;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.WriteResult;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A singleton class which manages the currently logged in user account.
 * 
 * Functions which modify user account data all go here, since they should
 * (hopefully) only be acting on the currently authorized user account.
 * 
 * @author ndars
 */
public final class LocalUserAccount {
    
    // static features
    
    // the singleton instance
    private static LocalUserAccount instance = null;
    
    /**
     * Gets the instance of the LocalUserAccount class. 
     * @return the LocalUserAccount singleton instance
     */
    public static LocalUserAccount getInstance() {
        if(null == instance) {
            instance = new LocalUserAccount();
        }
        return instance;
    }
   
    /**
     * Private constructor, just to initialize the singleton.
     */
    private LocalUserAccount() {
        activeUser = null;
    }
    
    // non-static features
    private UserAccount activeUser;
    
    /**
     * "Logs in" the local user account.
     * @param account the UserAccount instance for the account we're logging in. Should be pulled directly from a Firestore query.
     */
    public void login(UserAccount account) {
        if(null == account) {
            throw new IllegalArgumentException();
        }
        activeUser = account;
    }
    
    /**
     * "Logs out" the local user account.
     * Should be called whenever we log out of the current account.
     */
    public void logout() {
        activeUser = null;
    }
    
    /**
     * Checks if there's currently an local user account instance being tracked.
     * @return true if so, false if not
     */
    public boolean isLoggedIn() {
        return null != activeUser;
    }
    
    /**
     * Grabs a reference to the active user account.
     * @return a reference to the activeUserAccount, or null. Check with isLoggedIn() first if you don't want nulls!
     */
    public UserAccount getUser() {
        return activeUser;
    }
    
    /**
     * Updates the user's biography field both locally and on the Firestore.
     * @param biography the user's new bio
     * @return true if successful, false if not
     */
    public boolean updateBiography(String newBio) {
        if(!isLoggedIn()) {
            return false;
        }
        // update the remote data
        DocumentReference userDoc = App.fstore.collection(UserAccount.USER_DB_NAME).document(activeUser.getUserID());
        ApiFuture<WriteResult> future = userDoc.update(UserAccount.BIOGRAPHY, newBio);
        try {
            WriteResult result = future.get();
        } catch (InterruptedException | ExecutionException ex) {
            return false;
        }
        // update the local data, if that worked
        UserAccount updatedUserAcc = new UserAccount(activeUser.getUserID(), newBio, activeUser.getAvatarURL(), activeUser.getGameData());
        this.activeUser = updatedUserAcc;
        return true;
    }
    
    /**
     * Updates the user's avatar URL field both locally and on the Firestore.
     * @param avatarURL a link to the user's new avatar
     * @return true if successful, false if not
     */
    public boolean updateAvatar(String newAvatarURL) {
        if(!isLoggedIn()) {
            return false;
        }
        // update the remote data
        DocumentReference userDoc = App.fstore.collection(UserAccount.USER_DB_NAME).document(activeUser.getUserID());
        ApiFuture<WriteResult> future = userDoc.update(UserAccount.AVATAR, newAvatarURL);
        try {
            WriteResult result = future.get();
        } catch (InterruptedException | ExecutionException ex) {
            return false;
        }
        // update the local data, if that worked
        UserAccount updatedUserAcc = new UserAccount(activeUser.getUserID(), activeUser.getBiography(), newAvatarURL, activeUser.getGameData());
        this.activeUser = updatedUserAcc;
        return true;
    }
    
    /**
     * Updates the user's game stats both locally and on the Firestore.
     * @param dataFieldName the name of the field being updated; e.g., snakeTime, tictactoeWins, etc
     * @param data the value to update the stat to. NOTE: this should be a type that Firebase likes (ints, Strings, things of that nature)
     * @return true if successful, false if not
     */
    public boolean updateGameData(String dataFieldName, Object data) {
        if(!isLoggedIn()) {
            return false;
        }
        // update the remote data
        DocumentReference userDoc = App.fstore.collection(UserAccount.USER_DB_NAME).document(activeUser.getUserID());
        ApiFuture<WriteResult> future = userDoc.update(UserAccount.GAMEDATA + "." + dataFieldName, data);
        try {
            WriteResult result = future.get();
        } catch (InterruptedException | ExecutionException ex) {
            return false;
        }
        // update the local data, if that worked
        HashMap<String, Object> gameData = activeUser.getGameData();
        gameData.put(dataFieldName, data);
        return true;
    }
    
    /**
     * Increment the playtime stat based on a Timer instance.
     * Some notes:
     *  - if the timer never started in the first place, it returns false
     *  - if the timer is still running, this will stop the timer and record the time from there
     * @param timer the timer to record the playtime from
     * @return true if successful, false if not
     */
    public boolean recordTime(Timer timer) {
        if(!isLoggedIn() || null == timer || null == timer.getStartTime()) {
            return false;
        } else if(timer.isRunning()) {
            timer.stop();
        }
        // calculate the duration to update
        Duration timeElapsed = timer.getDuration();
        long timeElapsedSeconds = timeElapsed.getSeconds();
        // increment the remote data
        String timeFieldName = timer.getTrackedGame().toString() + "_time";
        DocumentReference userDoc = App.fstore.collection(UserAccount.USER_DB_NAME).document(activeUser.getUserID());
        ApiFuture<WriteResult> future = userDoc.update(UserAccount.GAMEDATA + "." + timeFieldName, FieldValue.increment(timeElapsedSeconds));
        try {
            WriteResult result = future.get();
        } catch (InterruptedException | ExecutionException ex) {
            return false;
        }
        // update the local data, if that worked
        HashMap<String, Object> gameData = activeUser.getGameData();
        long currentTime = (long)gameData.getOrDefault(timeFieldName, (long)0);
        gameData.put(timeFieldName, currentTime + timeElapsedSeconds);
        return true;
    }
}
