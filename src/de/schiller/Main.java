package de.schiller;

import de.schiller.gui.MainFrame;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args){

        String ip = JOptionPane.showInputDialog("Server IP:", "192.168.2.29");

        MainFrame app = new MainFrame(ip);


    }
}
