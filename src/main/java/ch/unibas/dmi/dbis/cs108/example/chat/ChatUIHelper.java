package ch.unibas.dmi.dbis.cs108.example.chat;

import javax.swing.*;
import java.awt.*;

/**
 * {@code ChatUIHelper} provides utility methods to integrate and manage a chat panel
 * within a given {@link JFrame}. It includes a toggle button to show or hide the chat panel
 * and requests focus appropriately.
 */
public class ChatUIHelper {

    /**
     * Installs the chat UI into the provided {@link JFrame}. A toggle button is placed at the
     * top for showing or hiding the chat panel, which is initially displayed.
     *
     * <p>
     * The provided {@code panel} should ideally be an instance of {@link ChatPanel}. If not,
     * the singleton {@code ChatPanel} instance is retrieved instead.
     * </p>
     *
     * @param frame The main application frame into which the chat UI will be installed.
     * @param panel The {@link JPanel} that is presumed to be the chat panel.
     *              If it is not a {@link ChatPanel}, the singleton instance is used.
     */
    public static void installChatUI(JFrame frame, JPanel panel) {
        // Ensure we use the singleton ChatPanel instance.
        ChatPanel chatPanel;
        if (panel instanceof ChatPanel) {
            chatPanel = (ChatPanel) panel;
        } else {
            // If the passed panel is not a ChatPanel, force using the singleton instance.
            chatPanel = ChatPanel.getInstance(null);
        }
        //System.out.println("Installing ChatPanel instance: " + chatPanel.hashCode());

        // Create a top panel for the toggle button.
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton toggleButton = new JButton("Close Chat"); // Chat is open by default
        topPanel.add(toggleButton);
        frame.add(topPanel, BorderLayout.NORTH);

        // Chat is visible at startup.
        chatPanel.setVisible(true);
        frame.add(chatPanel, BorderLayout.SOUTH);

        // Request focus on the chat input field at startup.
        JTextField chatInputField = chatPanel.getInputField();
        if (chatInputField != null) {
            chatInputField.requestFocusInWindow();
        }

        // Toggle the chat panel's visibility when the button is clicked.
        toggleButton.addActionListener(e -> {
            boolean visible = chatPanel.isVisible();
            chatPanel.setVisible(!visible);
            toggleButton.setText(visible ? "Open Chat" : "Close Chat");

            if (!visible) {
                // Chat just opened, focus on the chat input field if available.
                if (chatInputField != null) {
                    chatInputField.requestFocusInWindow();
                } else {
                    chatPanel.requestFocusInWindow();
                }
            } else {
                // Chat just closed, focus back on the game frame.
                frame.requestFocusInWindow();
            }
            // Refresh layout just in case.
            frame.revalidate();
        });
    }
}
