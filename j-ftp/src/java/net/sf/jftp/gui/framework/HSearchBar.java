package net.sf.jftp.gui.framework;

import java.awt.*;
import javax.swing.*;

import net.sf.jftp.config.Settings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class HSearchBar extends JPanel {
    private JLabel label;
    private JTextField searchField;
    private HImageButton searchButton;

    public HSearchBar(String labelText, ActionListener dir) {
        setLayout(new BorderLayout(5, 5));

        searchField = new JTextField(""); // Set the desired width
        add("Center", searchField);

        searchButton = new HImageButton(Settings.searchImage, "search",
                "Search directory", dir);
        searchButton.setToolTipText("Search directory");
        add("East", searchButton);

        setVisible(true);
        
        setPreferredSize(new Dimension(150, 25));
    }

    public String getLabel() {
        return label.getText();
    }

    public void setLabel(String labelText) {
        label.setText(labelText + "  ");
    }

    public String getSearchText() {
        return searchField.getText();
    }

    public void setSearchText(String searchText) {
        searchField.setText(searchText);
    }
    
    public JButton getSearchButton() {
    	return searchButton;
    }
}