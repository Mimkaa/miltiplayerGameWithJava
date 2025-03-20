package chat;

import javax.swing.*;
import java.awt.*;

public class ChatUIHelper {
    /**
     * Installs the chat UI into the provided JFrame.
     * This method adds a toggle button at the top to show/hide the chat panel.
     *
     * @param frame     the main application JFrame
     * @param chatPanel the ChatPanel instance to install
     */
    public static void installChatUI(JFrame frame, JPanel chatPanel) {
        // Create a top panel for the toggle button.
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton toggleButton = new JButton("Open Chat");
        topPanel.add(toggleButton);
        frame.add(topPanel, BorderLayout.NORTH);

        // Initially hide the chat panel.
        chatPanel.setVisible(false);
        frame.add(chatPanel, BorderLayout.SOUTH);

        // Toggle the chat panel's visibility when the button is clicked.
        toggleButton.addActionListener(e -> {
            boolean visible = chatPanel.isVisible();
            chatPanel.setVisible(!visible);
            toggleButton.setText(visible ? "Open Chat" : "Close Chat");
            // Optionally, transfer focus to the chat panel or back to the game.
            if (!visible) {
                chatPanel.requestFocusInWindow();
            } else {
                frame.requestFocusInWindow();
            }
            frame.revalidate();
        });
    }
}
