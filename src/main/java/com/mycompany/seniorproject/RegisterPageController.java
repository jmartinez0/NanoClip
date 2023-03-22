package com.mycompany.seniorproject;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author chriscanenguez & juan
 */
public class RegisterPageController implements Initializable {

    @FXML
    private TextField usernameField;
    
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField passwordField2;

    @FXML
    private Label errorLabel;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);
    }

    @FXML
    public void goToLoginPage() throws IOException {
        // Retrieves Loader for Login page.
        App.setRoot("LoginPage");
    } // End goToLoginPage.

    @FXML
    public void register() throws IOException {
        createUser();
        createUserDocument();
    }

    public void createUser() throws IOException {
        if (!passwordField.getText().equals(passwordField2.getText())) {
            errorLabel.setText("Password fields must match");
            errorLabel.setVisible(true);
            return;
        }
        try {
            //Make a new user record create request
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setUid(usernameField.getText().trim())
                    .setEmail(emailField.getText().trim())
                    .setPassword(passwordField.getText().trim())
                    .setDisplayName(usernameField.getText().trim());
            //Make a new UserRecord instance and use the data from the create request
            UserRecord newUser = FirebaseAuth.getInstance().createUser(request);
            //Use custom claims to set the new user's permissions to user and not admin
            Map<String, Object> claims = new HashMap<>();
            claims.put("user", true);
            FirebaseAuth.getInstance().setCustomUserClaims(newUser.getUid(), claims);
            //If registration is successful return to the log in page
            App.setRoot("LoginPage");
        } catch (FirebaseAuthException ex) {
            errorLabel.setText("Username is already in use");
            errorLabel.setVisible(true);
        } catch (IllegalArgumentException iae) {
            if (usernameField.getText().equals("") || passwordField.getText().equals("") || emailField.getText().equals("")) {
                errorLabel.setText("All fields must be filled out");
                errorLabel.setVisible(true);
            } else {
                errorLabel.setText("Password must be at least 6 characters long");
                errorLabel.setVisible(true);
            }
        }
    }

    public void createUserDocument() {
        //Create a new document using the username typed in the usernameField
        String username = usernameField.getText();
        String password = passwordField.getText();
        DocumentReference docRef = App.fstore.collection("Users").document(username);
        // make a default user account & add the password to their document
        UserAccount newUser = new UserAccount(username);
        docRef.set(newUser);
        try {
            Thread.sleep(1000); // we can only update the same document once a second
        } catch (InterruptedException ex) {}
        docRef.update("Password", password);
    }

}
