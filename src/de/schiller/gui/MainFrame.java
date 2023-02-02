package de.schiller.gui;

import de.schiller.math.Smooth;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class MainFrame extends JPanel implements MouseMotionListener, MouseListener, ActionListener, Runnable{

    private JFrame fenster;

    private JPanel mapsFrame, beliveFrame, confidenceFrame, overviewFrame, controlFrame, settingsFrame;

    private Image missionIMG, disasterIMG, humanIMG, droneIMG, logoIMG, logo2IMG;

    private JLabel disasterLabel, humanLabel, droneLabel, healthLabel;

    public JButton setB, resetB;

    public JToggleButton markB, atracktB, avoidB;

    //Simulation
    private volatile int health = 100;
    private int knownDesasters = 0;
    private volatile float[][] confidenceMap = new float[40][40];
    private volatile float[][] belifeMap = new float[40][40];
    private volatile float[][] markerMap = new float[40][40];
    private volatile Point humanPos = new Point(0,0);

    //Network
    private String ip;
    private Thread network;
    private DatagramSocket socketUDP;
    private Socket socketTCP;
    private InetAddress address;
    private int portUDP = 5500;
    private int portTCP = 65432;
    private byte[] buf = new byte[65000];

    //GUI
    private Point pos = new Point(0, 0);
    private boolean isDragged = false;
    private boolean isClicked = false;
    private boolean isMarked = false;
    private boolean isAtrackt = true;

    public MainFrame(String ip) {
        fenster = new JFrame("Arcade swarm GUI - " + ip);
        fenster.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fenster.setSize(new Dimension(1360, 1000));
        fenster.setResizable(false);
        fenster.setLocationRelativeTo(null);
        fenster.setContentPane(this);
        fenster.setDefaultLookAndFeelDecorated(true);
        super.setLayout(null);
        super.addMouseMotionListener(this);
        super.addMouseListener(this);

        addElements();

        fenster.setVisible(true);

        initMarkerMap();

        //Start Network
        try {
            this.ip = ip;
            //UDP
            socketUDP = new DatagramSocket();
            address = InetAddress.getByName(ip);
            byte[] tmp = new byte[1024];
            DatagramPacket packet = new DatagramPacket(tmp, tmp.length, address, portUDP);
            socketUDP.send(packet);
            System.out.println("UDP connected!");

            //TCP
            Thread.sleep(200);
            socketTCP = new Socket(ip, portTCP);
            System.out.println("TCP connected!");

            //Thread
            network = new Thread(this);
            network.start();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    public void addElements() {
        mapsFrame = new JPanel();
        mapsFrame.setBorder(BorderFactory.createTitledBorder("Mission:"));
        mapsFrame.setSize(new Dimension(1340, 955));
        mapsFrame.setLocation(10, 10);
        mapsFrame.setLayout(null);

        beliveFrame = new JPanel();
        beliveFrame.setBorder(BorderFactory.createTitledBorder("Belive map:"));
        beliveFrame.setSize(new Dimension(640, 650));
        beliveFrame.setLocation(20, 20);
        mapsFrame.add(beliveFrame);

        confidenceFrame = new JPanel();
        confidenceFrame.setBorder(BorderFactory.createTitledBorder("Confidence map:"));
        confidenceFrame.setSize(new Dimension(640, 650));
        confidenceFrame.setLocation(680, 20);
        mapsFrame.add(confidenceFrame);

        overviewFrame = new JPanel();
        overviewFrame.setBorder(BorderFactory.createTitledBorder("Übersicht:"));
        overviewFrame.setSize(new Dimension(240, 270));
        overviewFrame.setLocation(20, 670);
        overviewFrame.setLayout(null);
        mapsFrame.add(overviewFrame);

        controlFrame = new JPanel();
        controlFrame.setLayout(new GridLayout(3, 1));
        controlFrame.setBorder(BorderFactory.createTitledBorder("Steuerung:"));
        controlFrame.setSize(new Dimension(770, 270));
        controlFrame.setLocation(270, 670);
        mapsFrame.add(controlFrame);

        settingsFrame = new JPanel();
        settingsFrame.setLayout(new GridLayout(1, 3));
        settingsFrame.setBorder(BorderFactory.createTitledBorder("Einstellungen:"));
        settingsFrame.setSize(new Dimension(270, 150));
        settingsFrame.setLocation(1050, 670);
        mapsFrame.add(settingsFrame);

        missionIMG = Toolkit.getDefaultToolkit().getImage("resources/disaster_scr.png");
        disasterIMG = Toolkit.getDefaultToolkit().getImage("resources/disaster.png");
        humanIMG = Toolkit.getDefaultToolkit().getImage("resources/human.png");
        droneIMG = Toolkit.getDefaultToolkit().getImage("resources/drone.png");
        logoIMG = Toolkit.getDefaultToolkit().getImage("resources/logo.png");
        logo2IMG = Toolkit.getDefaultToolkit().getImage("resources/logo2.png");

        disasterLabel = new JLabel("10");
        disasterLabel.setFont(new Font("Arial", Font.BOLD, 40));
        disasterLabel.setLocation(100, 20);
        disasterLabel.setSize(100, 100);
        overviewFrame.add(disasterLabel);

        humanLabel = new JLabel("10");
        humanLabel.setFont(new Font("Arial", Font.BOLD, 40));
        humanLabel.setLocation(100, 90);
        humanLabel.setSize(100, 100);
        overviewFrame.add(humanLabel);

        droneLabel = new JLabel("10");
        droneLabel.setFont(new Font("Arial", Font.BOLD, 40));
        droneLabel.setLocation(100, 160);
        droneLabel.setSize(100, 100);
        overviewFrame.add(droneLabel);

        healthLabel = new JLabel("Health:");
        healthLabel.setLocation(170, -20);
        healthLabel.setSize(100, 100);
        overviewFrame.add(healthLabel);

        markB = new JToggleButton("Bereich markieren");
        markB.addActionListener(this);
        controlFrame.add(markB);

        atracktB = new JToggleButton(new ImageIcon("resources/atrackt.png"));
        atracktB.addActionListener(this);
        atracktB.setSelected(true);
        settingsFrame.add(atracktB);

        avoidB = new JToggleButton(new ImageIcon("resources/avoid.png"));
        avoidB.addActionListener(this);
        settingsFrame.add(avoidB);


        resetB = new JButton("Auswahl löschen");
        resetB.addActionListener(this);
        resetB.setEnabled(false);
        controlFrame.add(resetB);

        setB = new JButton("Bereich anwenden");
        setB.addActionListener(this);
        setB.setEnabled(false);
        controlFrame.add(setB);
        super.add(mapsFrame);
    }

    public void update(){
        //Calculate desasters
        knownDesasters = 0;
        for(int x = 0; x < 40; x++){
            for(int y = 0; y < 40; y++){
                if(belifeMap[x][y] >= 0.7f) knownDesasters++;
            }
        }
        knownDesasters = knownDesasters/9;
        disasterLabel.setText(((knownDesasters < 10) ? "0" : "") + knownDesasters);
    }

    @Override
    public void run() {
        while(true){
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socketUDP.receive(packet);
                JSONObject json = new JSONObject(new String(buf));

                health = (int) (100 - json.getFloat("health"));

                int known_human = json.getInt("humans");
                humanLabel.setText(((known_human < 10) ? "0" : "") + known_human);

                int known_drones = json.getInt("known_drones");
                droneLabel.setText(((known_drones < 10) ? "0" : "") + known_drones);

                JSONArray conf = json.getJSONArray("confidence_map");
                JSONArray belf = json.getJSONArray("internal_map");

                for (int x = 0; x < 40; x++) {
                    JSONArray conf1 = conf.getJSONArray(x);
                    JSONArray belf1 = belf.getJSONArray(x);
                    for (int y = 0; y < 40; y++) {
                        confidenceMap[x][y] = conf1.getFloat(y);
                        belifeMap[x][y] = belf1.getFloat(y);
                    }
                }

            } catch (Exception x) {
                x.printStackTrace();
            }
        }
    }

    public void initMarkerMap(){
        for(int x = 0; x < 40; x++){
            for(int y = 0; y < 40; y++){
                markerMap[x][y] = isAtrackt ? 1 : 0;
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        g.drawImage(missionIMG, 50, 60, 600, 600, null);
        g.drawImage(missionIMG, 710, 60, 600, 600, null);

        g.drawImage(disasterIMG, 50, 725, 50, 50, null);
        g.drawImage(humanIMG, 50, 795, 50, 50, null);
        g.drawImage(droneIMG, 50, 865, 50, 50, null);
        g.drawImage(logoIMG, 1145, 910, 162, 32, null);
        g.drawImage(logo2IMG, 1035, 830, null);

        //Draw Health
        g.setColor(Color.GREEN);
        g.fillRect(200, 920 - 2 * health, 50, 2 * health);
        g.setColor(Color.BLACK);
        g.drawRect(200, 720, 50, 200);

        //Draw belive map and confidence map
        g.setColor(new Color(0, 0, 0, 0.5f));
        g.fillRect(50, 60, 600, 600);

        for (int x = 0; x < 40; x++) {
            for (int y = 0; y < 40; y++) {
                //Draw Confidence
                if (confidenceMap[y][x] >= 0.955f) {
                    g.setColor(getColor(confidenceMap[y][x]));
                    g.fillRect(x * 15 + 710, y * 15 + 60, 15, 15);
                }
                //Draw belive
                if (belifeMap[y][x] >= 0.7f) {
                    g.setColor(new Color(1, 0, 0, 0.8f));
                    g.fillRect(x * 15 + 50, y * 15 + 60, 15, 15);
                }
                //Draw marker
                if(markerMap[x][y] == 0){
                    g.setColor(new Color(1,1,1, 0.4f));
                    g.fillRect(x * 15 + 50, y * 15 + 60, 15, 15);
                }

            }
        }

        //Draw Mouse
        drawMouse(g);

        update();

        super.repaint();
    }

    public Color getColor(float value){
        if(value >= 0.98f) return new Color(value, value, 0, 0.6f);
        if(value >= 0.98f) return new Color(value/2, value, 0, 0.6f);
        if(value >= 0.975f) return new Color(value/4, value, 0, 0.6f);
        if(value >= 0.97f) return new Color(value/6, value, 0, 0.6f);
        if(value >= 0.965f) return new Color(value/8, value, 0, 0.6f);
        if(value >= 0.96f) return new Color(0, value, 0, 0.6f);
        return new Color(0, 0, value, 0.8f);
    }

    public void drawMouse(Graphics g) {
        if(isMarked){
            if (!(pos.getX() >= 50 && pos.getY() >= 60 && pos.getX() < 650 && pos.getY() < 660)) return;

            int x = pos.x - 50;
            int y = pos.y - 60;

            final int tileX = x / 15;
            final int tileY = y / 15;

            g.setColor(new Color(1,1,1, 0.7f));
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    if (!(50 + 15 * (tileX + i) >= 50 && 60 + 15 * (tileY + j) >= 60 && 50 + 15 * (tileX + i) < 650 && 60 + 15 * (tileY + j) < 660)) continue;
                    g.fillRect(50 + 15 * (tileX + i), 60 + 15 * (tileY + j), 15, 15);
                    if(isDragged) markerMap[tileX + i][tileY + j] = isAtrackt ? 0 : 1;
                    if(isClicked) markerMap[tileX + i][tileY + j] = isAtrackt ? 0 : 1;
                }
            }
            isClicked = false;
        }else initMarkerMap();
    }

    private String[] splitByNumber(String s, int size) {
        if(s == null || size <= 0)
            return null;
        int chunks = s.length() / size + ((s.length() % size > 0) ? 1 : 0);
        String[] arr = new String[chunks];
        for(int i = 0, j = 0, l = s.length(); i < l; i += size, j++)
            arr[j] = s.substring(i, Math.min(l, i + size));
        return arr;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        pos.setLocation(e.getX(), e.getY());
        isDragged = true;

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        pos.setLocation(e.getX(), e.getY());
        isDragged = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        pos.setLocation(e.getX(), e.getY());
        if (!(pos.getX() >= 50 && pos.getY() >= 60 && pos.getX() < 650 && pos.getY() < 660)) return;
        isClicked = true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == resetB){
            initMarkerMap();
            return;
        }
        if(e.getSource() == markB){
            isMarked = markB.isSelected();
            resetB.setEnabled(markB.isSelected());
            setB.setEnabled(markB.isSelected());
        }
        if(e.getSource() == setB){
           markerMap = Smooth.smooth(markerMap);

            try {
                OutputStream output = socketTCP.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                JSONArray parrent = new JSONArray();
                for(int i = 0; i < 40; i++){
                    JSONArray child = new JSONArray();
                    for(int j = 0; j < 40; j++){
                        child.put(markerMap[j][i]);
                    }
                    parrent.put(child);
                }
                
                writer.println(parrent);
                System.out.println("Send MarkerMap to Server!");
                markB.setSelected(false);
                resetB.setEnabled(false);
                setB.setEnabled(false);
                isMarked = false;
                initMarkerMap();

            } catch (Exception ioException) {
                ioException.printStackTrace();
            }
        }
        if(e.getSource() == atracktB){
            if(atracktB.isSelected()){
                avoidB.setSelected(false);
                isAtrackt = true;
                initMarkerMap();
            }else{
                atracktB.setSelected(true);
            }
        }
        if(e.getSource() == avoidB){
            if(avoidB.isSelected()){
                atracktB.setSelected(false);
                isAtrackt = false;
                initMarkerMap();
            }else{
                avoidB.setSelected(true);
            }

        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }



}


