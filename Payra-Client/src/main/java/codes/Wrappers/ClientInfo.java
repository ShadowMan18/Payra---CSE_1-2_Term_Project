package codes.Wrappers;

import java.io.Serializable;

public class ClientInfo implements Serializable {
    private String firstName;
    private String lastName;
    private String id;
    private String password;
    private String recoveryQuestion;
    private String recoveryAnswer;
    private byte[] profilePicture;

    public ClientInfo(String firstName, String lastName, String id, String password, String recoveryQuestion, String recoveryAnswer, byte[] profilePicture) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.id = id;
        this.password = password;
        this.recoveryQuestion = recoveryQuestion;
        this.recoveryAnswer = recoveryAnswer;
        this.profilePicture = profilePicture;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public String getRecoveryQuestion() {
        return recoveryQuestion;
    }

    public String getRecoveryAnswer() {
        return recoveryAnswer;
    }

    public byte[] getProfilePicture() {
        return profilePicture;
    }

    public String getFullName(){
        return firstName+" "+lastName;
    }
}
