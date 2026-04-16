/**
 * Import the java packages used to drive the JJ's automation program.
 * The Swing and AWT packages are used for building the GUI of the program
 * The IO package is used for reading to and writing from the file that contains employee information
 * The ArrayList package is used for access to a dynamically sized data structure to contain information
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class JJMain{
    public static void main(String[] args){
        /**
         * Initialize a set of arraylists to store information the program will need ready access to
         */
        ArrayList<String[]> validLogins = new ArrayList<>(); ///storage for the current list of active logins
        ArrayList<Ticket> activeOrders = new ArrayList<>(); ///storage for all the current orders

        /// add a test login to the validLogin list
        String[] test1 = new String[2];
        test1[0] = "homer";
        test1[1] = "123567";
        validLogins.add(test1);

        /**
         * Initialize a set of action listeners that handle common button uses like exiting the program
         * The exit listener defines the program exit - when called, it will stop program execution
         */
        ActionListener exit = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };

        /**
         * Initialize the startup screen for the program - this screen displays a welcome message and provides the options to exit the program, move to account creation, or move to the login screen
         * the JLabel startWelcome handles the welcome message
         */
        JFrame startup = new JFrame("JJ's Automation Startup");
        startup.setExtendedState(6); ///extendedState(6) is the full screen indicator - this value will be referenced throughout the program in frame creation
        startup.setBackground(Color.LIGHT_GRAY);

        JLabel startWelcome = new JLabel("<html>Welcome to JJ's Restaurant!<br/>Please choose an option below<html/>", SwingConstants.CENTER);
        startWelcome.setSize(500, 250);
        startWelcome.setLocation(100, 75);
        startup.add(startWelcome);

        /**
         * Initialize both the account creation and login screen frames
         */
        JFrame createAcc = new JFrame("JJ's Account Creation");
        createAcc.setExtendedState(6);
        createAcc.setBackground(Color.LIGHT_GRAY);

        JLabel accCreateGuide = new JLabel();
        accCreateGuide.setSize(500, 250);
        accCreateGuide.setLocation(100, 75);
        accCreateGuide.setText("Please enter your new username and password below");
        createAcc.add(accCreateGuide);

        JFrame accLogin = new JFrame("JJ's Account Login");
        accLogin.setExtendedState(6);
        accLogin.setBackground(Color.LIGHT_GRAY);

        /**
         * Initialize the startup page button to take the user to the account creation page
         * This includes a new action listener, which will move the user to the account creation page when pressed
         */
        JButton moveSignin = new JButton();
        moveSignin.setText("Sign Up");
        moveSignin.setSize(245, 200);
        moveSignin.setLocation(100, 330);
        moveSignin.setBackground(Color.WHITE);
        moveSignin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startup.setVisible(false);
                createAcc.setLayout(null);
                createAcc.setVisible(true);
            }
        });
        startup.add(moveSignin);

        /**
         * Initialize the startup page button to take the user to the login screen
         * This includes a new action listener which moves the user to the login screen on button press
         */
        JButton moveLogin = new JButton();
        moveLogin.setText("Login");
        moveLogin.setSize(245, 200);
        moveLogin.setLocation(350, 330);
        moveLogin.setBackground(Color.WHITE);
        moveLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startup.setVisible(false);
                accLogin.setLayout(null);
                accLogin.setVisible(true);
            }
        });
        startup.add(moveLogin);

        /**
         * Initialize the startup page exit button
         * This button utilizes the exit action listener
         */
        JButton startupExit = new JButton();
        startupExit.setText("Close Program");
        startupExit.setSize(500, 200);
        startupExit.setLocation(100, 540);
        startupExit.setBackground(Color.RED);
        startupExit.addActionListener(exit);
        startup.add(startupExit);

        /**
         * Initialize the account creation and login page exit and home buttons
         */
        JButton accCreateExit = new JButton();
        accCreateExit.setText("Exit");
        accCreateExit.setBackground(Color.RED);
        accCreateExit.setSize(250, 200);
        accCreateExit.setLocation(100, 540);
        accCreateExit.addActionListener(exit);
        createAcc.add(accCreateExit);

        JButton accHome = new JButton("Home");
        accHome.setSize(250, 200);
        accHome.setLocation(355, 540);
        accHome.setBackground(Color.GREEN);
        accHome.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createAcc.setVisible(false);
                startup.setLayout(null);
                startup.setVisible(true);
            }
        });
        createAcc.add(accHome);


        JButton loginExit = new JButton();
        loginExit.setText("Exit");
        loginExit.setBackground(Color.RED);
        loginExit.setSize(250, 150);
        loginExit.setLocation(100, 590);
        loginExit.addActionListener(exit);
        accLogin.add(loginExit);

        JButton loginHome = new JButton("Home");
        loginHome.setSize(250, 150);
        loginHome.setLocation(355, 590);
        loginHome.setBackground(Color.GREEN);
        loginHome.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                accLogin.setVisible(false);
                startup.setLayout(null);
                startup.setVisible(true);
            }
        });
        accLogin.add(loginHome);

        /**
         * Initialize a basic test home screen for the program with success message and exit button
         */
        JFrame testHome = new JFrame("JJ's Test Home");
        testHome.setExtendedState(6);
        testHome.setBackground(Color.LIGHT_GRAY);

        JLabel success = new JLabel("Login Successful!");
        success.setSize(200, 100);
        success.setLocation(100, 100);
        success.setBackground(Color.GREEN);
        testHome.add(success);

        JButton testHomeExit = new JButton("Exit");
        testHomeExit.setSize(200, 100);
        testHomeExit.setLocation(100, 305);
        testHomeExit.setBackground(Color.RED);
        testHome.add(testHomeExit);

        /**
         * Initialize text and password fields for account creation, along with labels
         */
        JLabel createUNLabel = new JLabel();
        createUNLabel.setSize(75, 50);
        createUNLabel.setLocation(100, 330);
        createUNLabel.setText("Username:");
        createAcc.add(createUNLabel);

        JTextField createUsername = new JTextField();
        createUsername.setSize(445, 50);
        createUsername.setLocation(180, 330);
        createUsername.setBackground(Color.WHITE);
        createAcc.add(createUsername);

        JLabel createPWLabel = new JLabel();
        createPWLabel.setSize(75, 50);
        createPWLabel.setLocation(100, 375);
        createPWLabel.setText("Password:");
        createAcc.add(createPWLabel);

        JPasswordField createPassword = new JPasswordField();
        createPassword.setSize(445, 50);
        createPassword.setLocation(180, 375);
        createPassword.setBackground(Color.WHITE);
        createAcc.add(createPassword);

        /**
         * Initialize the button to create an account
         * This includes a new action listener which checks the format of the username and password and presents an error if one or both are invalid
         */
        JButton createAccount = new JButton("Create Account");
        createAccount.setSize(500, 150);
        createAccount.setLocation(100, 800);
        createAccount.setBackground(Color.GRAY);
        createAccount.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /// store the attempted username and password
                String attemptUN = createUsername.getText();
                char[] attemptPW = createPassword.getPassword();
                String attemptPWs = java.util.Arrays.toString(attemptPW);
                String[] attemptLogin = {attemptUN, attemptPWs};

                /// The first check the action listener makes is if the username is already on file
                if (!validLogins.contains(attemptLogin)) {
                    /// The next check will be if the password is too long or short (passwords should be 6 character)
                    if (attemptPW.length == 6) {
                        /**
                         * The final check will be for sequential (ABCDEF format) and uniform (AAAAAA format) passwords
                         * This check functions by checking pairs of adjacent elements and checking for the above formats
                         * If each pair satisfies THE SAME invalid format, do not allow the password
                         */
                        boolean invalidFormat = false; ///flag for invalid password
                        int uniform = 0; ///counter for uniform pairs
                        int inSequenceUp = 0; ///counter for sequential pairs
                        int inSequenceDn = 0; ///second counter for sequential pairs

                        for (int i = 0; i < 5; i++) {
                            if (attemptPW[i] == attemptPW[i+1]) {
                                uniform++; ///pair is uniform
                            }

                            else if (attemptPW[i] == attemptPW[i + 1] + 1) {
                                inSequenceUp++; ///pair is in sequence
                            }

                            else if (attemptPW[i] == attemptPW[i+1] - 1) {
                                inSequenceDn++; ///pair is in sequence
                            }
                        }

                        if (uniform == 5 || inSequenceUp == 5 || inSequenceDn == 5) {
                            /// if any of the counters have a value of 5, this indicates an invalid password format
                            invalidFormat = true;
                        }

                        if (invalidFormat) {
                            /// If the invalid format flag is true generate an error popup
                            JOptionPane.showMessageDialog(createAcc, "<html>Error: Password cannot be uniform (AAAAAA) or sequential (ABCDEF).<br/>Please try again,html/>");
                        }

                        else {
                            /// If the invalid format flag is false, add the provided username and password to username and password lists then move to the login screen
                            validLogins.add(attemptLogin);
                            System.out.println("Saved password: " + attemptPWs);

                            createAccount.setVisible(false);
                            accLogin.setLayout(null);
                            accLogin.setVisible(true);
                        }
                    }

                    else {
                        JOptionPane.showMessageDialog(createAcc, "<html>Error: Password needs to be exactly 6 characters.<br/>Please try again<html/>");
                    }
                }

                else {
                    JOptionPane.showMessageDialog(createAcc, "<html>Error: Username is already on file.<br/>Please try again<html/>");
                }
            }
        });
        createAcc.add(createAccount);

        /**
         * Initialize text fields and account login button
         */
        JLabel loginUsername = new JLabel("Username:");
        loginUsername.setSize(75, 50);
        loginUsername.setLocation(100, 330);
        accLogin.add(loginUsername);

        JLabel loginPassword = new JLabel("Password:");
        loginPassword.setSize(75, 50);
        loginPassword.setLocation(100, 385);
        accLogin.add(loginPassword);

        JTextField loginUNinput = new JTextField();
        loginUNinput.setSize(420, 50);
        loginUNinput.setLocation(180, 330);
        accLogin.add(loginUNinput);

        JPasswordField loginPWinput = new JPasswordField();
        loginPWinput.setSize(420, 50);
        loginPWinput.setLocation(180, 385);
        accLogin.add(loginPWinput);

        /**
         * initialize account login button
         * this includes an action listener which compares the inputs in username/password fields against current list of active accounts
         */
        JButton login = new JButton("Login");
        login.setSize(500, 150);
        login.setLocation(100, 800);
        login.setBackground(Color.GRAY);
        login.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String inputUN = loginUNinput.getText();
                String inputPWs = java.util.Arrays.toString(loginPWinput.getPassword());

                String[] inputLogin = {inputUN, inputPWs};

                if (validLogins.contains(inputLogin)) {
                    accLogin.setVisible(false);
                    testHome.setLayout(null);
                    testHome.setVisible(true);
                }

                else {
                    JOptionPane.showMessageDialog(accLogin, "<html>Error: Invalid username or password.<br/>Please try again<html/>");
                }
            }
        });
        accLogin.add(login);

        startup.setLayout(null);
        startup.setVisible(true);
    }
}