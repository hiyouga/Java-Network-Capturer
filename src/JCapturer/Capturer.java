/*
 * Java Network Capturer
 * @Package: JCapturer
 * @Author: Yw Zheng, Ys Lv, Sy Ren, Yx Du
 * @Date: 2018-11-11
 */

package JCapturer;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import jpcap.PacketReceiver;
import jpcap.packet.*;

public class Capturer {
    // UI components
    JMenuItem newFileItem;
    JMenuItem saveFileItem;
    JMenuItem versionInfoItem;
    JButton searchDevices;
    JButton startCapturer;
    JButton stopCapturer;
    JLabel deviceInfo;
    JLabel statusLabel;
    JScrollPane packagePanel;
    JTable packageTable;
    DefaultTableModel mainTable;
    JFrame versionFrame;
    // Captor
    JpcapCaptor captor;
    NetworkInterface currentDevice;
    // Global Variable
    int packageCounter;
    PrintWriter printWriter;
    DataOutputStream dataOutput;

    public Capturer() {
        createFrame();
    }

    public void createFrame() {
        // Menu Bar
        JMenuBar menuBar = new JMenuBar();
        JMenu mFile = new JMenu("File");
        JMenu mVer = new JMenu("Version");
        newFileItem = new JMenuItem("New File");
        saveFileItem = new JMenuItem("Save To");
        versionInfoItem = new JMenuItem("Version Info");
        mFile.add(newFileItem);
        mFile.add(saveFileItem);
        mVer.add(versionInfoItem);
        menuBar.add(mFile);
        menuBar.add(mVer);
        // Version Frame
        JLabel versionLabel = new JLabel("<html><div style=\"font-size:20px;text-align:center;\">V1.0.0<br>BUAA CSTer Presents<br>2018-11-11</div>");
        versionFrame = new JFrame("Version Info");
        versionFrame.setLayout(new FlowLayout(FlowLayout.CENTER));
        versionFrame.add(versionLabel);
        versionFrame.setSize(350, 180);
        versionFrame.setLocationRelativeTo(null);
        versionFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        versionFrame.setResizable(false);
        versionFrame.setVisible(false);
        // DeviceInfo
        JPanel devicePanel = new JPanel();
        devicePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        devicePanel.setSize(640, 100);
        devicePanel.setBorder(new TitledBorder("Device Info"));
        searchDevices = new JButton("Search Device");
        JLabel deviceLabel = new JLabel("Current Device:");
        deviceInfo = new JLabel("");
        devicePanel.add(searchDevices);
        devicePanel.add(deviceLabel);
        devicePanel.add(deviceInfo);
        // Settings
        JPanel settingPanel = new JPanel();
        devicePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        devicePanel.setSize(640, 100);
        settingPanel.setBorder(new TitledBorder("Settings"));
        startCapturer = new JButton("Start Capture");
        stopCapturer = new JButton("Stop Capture");
        settingPanel.add(startCapturer);
        settingPanel.add(stopCapturer);
        // Side Panel
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BorderLayout());
        sidePanel.setSize(1280, 100);
        sidePanel.add(devicePanel, BorderLayout.CENTER);
        sidePanel.add(settingPanel, BorderLayout.EAST);
        // Package Table
        String[] name = new String[]{"SERIAL_ID","SOURCE_MAC","DEST_MAC","SOURCE_IP","DEST_IP","SOURCE_PORT","DEST_PORT","PROTOCOL","CAP_LEN","TIME_STAMP"};
        mainTable = new DefaultTableModel(null, name);
        packageTable = new JTable(mainTable);
        packageTable.setEnabled(false);
        ((DefaultTableCellRenderer)packageTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
        DefaultTableCellRenderer render = new DefaultTableCellRenderer();
        render.setHorizontalAlignment(JLabel.CENTER);
        packageTable.setDefaultRenderer(Object.class, render);
        // Package Info
        packagePanel = new JScrollPane();
        packagePanel.setPreferredSize(new Dimension(1200, 580));
        packagePanel.setBorder(new TitledBorder("Package Content"));
        packagePanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        packagePanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        packagePanel.setViewportView(packageTable);
        // Status Bar
        JToolBar statusBar = new JToolBar();
        statusLabel = new JLabel("No device.");
        statusBar.setSize(1200, 40);
        statusBar.add(statusLabel);
        // Overall
        JFrame mainFrame = new JFrame("Capturer");
        mainFrame.setSize(1280, 720);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setJMenuBar(menuBar);
        mainFrame.add(sidePanel, BorderLayout.NORTH);
        mainFrame.add(packagePanel, BorderLayout.CENTER);
        mainFrame.add(statusBar, BorderLayout.SOUTH);
        mainFrame.setResizable(true);
        mainFrame.setVisible(true);
        addActionListener();
    }

    private void addActionListener() {
        searchDevices.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                deviceInfo.setText(getDevices());
            }
        });
        startCapturer.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (deviceInfo.getText().isEmpty()) {
                    deviceInfo.setText(getDevices());
                }
                try {
                    printWriter = new PrintWriter(new FileWriter(new File("packetInfo-" + System.currentTimeMillis() + ".txt")));
                    dataOutput = new DataOutputStream(new FileOutputStream(new File("packets-" + System.currentTimeMillis() + ".bin")));
                } catch (IOException err) {
                    err.printStackTrace();
                }
                new CapThread();
            }
        });
        stopCapturer.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                captor.breakLoop();
            }
        });
        newFileItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                packageCounter = 0;
                for (int row = packageTable.getRowCount() - 1; row >= 0; row--) {
                    mainTable.removeRow(row);
                }
            }
        });
        saveFileItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.showDialog(new JLabel(), "Select");
                if (chooser.getSelectedFile() != null) {
                    String path = chooser.getSelectedFile().getPath();
                    String[] tableHeader = new String[]{"SERIAL_ID","SOURCE_MAC","DEST_MAC","SOURCE_IP","DEST_IP","SOURCE_PORT","DEST_PORT","PROTOCOL","CAP_LEN","TIME_STAMP"};
                    String tableContent = "<style>table,th,td{border:1px solid #000;text-align:center;border-collapse:collapse;padding:5px;}</style><table><tr>";
                    for (int i = 0; i < tableHeader.length; i++) {
                        tableContent += "<th>" + tableHeader[i] + "</th>";
                    }
                    tableContent += "</tr>";
                    for (int row = 0; row < packageTable.getRowCount(); row++) {
                        tableContent += "<tr>";
                        for (int col = 0; col < packageTable.getColumnCount() ; col++) {
                            tableContent += "<td>" + packageTable.getValueAt(row, col) + "</td>";
                        }
                        tableContent += "</tr>";
                    }
                    tableContent += "</table>";
                    try {
                        String fileName = path + File.separatorChar + "packetTable-" + System.currentTimeMillis() + ".html";
                        PrintWriter dumpWriter = new PrintWriter(new FileWriter(new File(fileName)));
                        dumpWriter.write(tableContent);
                        dumpWriter.close();
                        statusLabel.setText("Saved as " + fileName);
                    } catch (IOException err) {
                        err.printStackTrace();
                    }
                }
            }
        });
        versionInfoItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                versionFrame.setVisible(true);
            }
        });
    }

    private void refreshTable() {
        int rowCount = packageTable.getRowCount();
        packageTable.getSelectionModel().setSelectionInterval(rowCount - 1, rowCount - 1);
        Rectangle rect = packageTable.getCellRect(rowCount - 1, 0, true);
        packageTable.scrollRectToVisible(rect);
    }

    private void dataWriter(byte[] data) {
        try {
            dataOutput.write(data);
            dataOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDevices() {
    	String localAddress = "0.0.0.0";
        try {
            localAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        final NetworkInterface[] devices = JpcapCaptor.getDeviceList();
        for (NetworkInterface device : devices) {
            for (NetworkInterfaceAddress addr : device.addresses) {
                if (addr.address.getHostAddress().equals(localAddress)) {
                    currentDevice = device;
                    statusLabel.setText("Device OK.");
                    return device.name;
                }
            }
        }
        return "Device Not Found";
    }

    public class Receiver implements PacketReceiver {
        public void receivePacket(Packet packet) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            printWriter.write(packet.toString());
            printWriter.println();
            packageCounter++;
            Vector<String> packetInfo = new Vector<String>();
            packetInfo.add(String.valueOf(packageCounter));
            DatalinkPacket datalinkPacket = packet.datalink;
            if (datalinkPacket instanceof EthernetPacket) {
                EthernetPacket ethernetPacket = (EthernetPacket)datalinkPacket;
                packetInfo.add(ethernetPacket.getSourceAddress());
                packetInfo.add(ethernetPacket.getDestinationAddress());
            }
            if (packet instanceof TCPPacket) {
                TCPPacket tcpPacket = (TCPPacket)packet;
                packetInfo.add(tcpPacket.src_ip.getHostAddress());
                packetInfo.add(tcpPacket.dst_ip.getHostAddress());
                packetInfo.add(String.valueOf(tcpPacket.src_port));
                packetInfo.add(String.valueOf(tcpPacket.dst_port));
                packetInfo.add("TCP");
                packetInfo.add(String.valueOf(tcpPacket.caplen));
                packetInfo.add(simpleDateFormat.format(new Date(new Long(tcpPacket.sec + "000"))));
                dataWriter(tcpPacket.data);
            } else if (packet instanceof UDPPacket) {
                UDPPacket udpPacket = (UDPPacket)packet;
                packetInfo.add(udpPacket.src_ip.getHostAddress());
                packetInfo.add(udpPacket.dst_ip.getHostAddress());
                packetInfo.add(String.valueOf(udpPacket.src_port));
                packetInfo.add(String.valueOf(udpPacket.dst_port));
                packetInfo.add("UDP");
                packetInfo.add(String.valueOf(udpPacket.caplen));
                packetInfo.add(simpleDateFormat.format(new Date(new Long(udpPacket.sec + "000"))));
                dataWriter(udpPacket.data);
            } else if (packet instanceof ICMPPacket) {
                ICMPPacket icmpPacket = (ICMPPacket)packet;
                packetInfo.add(icmpPacket.src_ip.getHostAddress());
                packetInfo.add(icmpPacket.dst_ip.getHostAddress());
                packetInfo.add("-");
                packetInfo.add("-");
                packetInfo.add("ICMP");
                packetInfo.add(String.valueOf(icmpPacket.caplen));
                packetInfo.add(simpleDateFormat.format(new Date(new Long(icmpPacket.sec + "000"))));
                dataWriter(icmpPacket.data);
            } else if (packet instanceof ARPPacket) {
                ARPPacket arpPacket = (ARPPacket)packet;
                packetInfo.add(arpPacket.getSenderProtocolAddress().toString());
                packetInfo.add(arpPacket.getTargetProtocolAddress().toString());
                packetInfo.add("-");
                packetInfo.add("-");
                packetInfo.add("ARP");
                packetInfo.add(String.valueOf(arpPacket.caplen));
                packetInfo.add(simpleDateFormat.format(new Date(new Long(arpPacket.sec + "000"))));
                dataWriter(arpPacket.data);
            }
            mainTable.addRow(packetInfo);
            refreshTable();
        }
    }

    public class CapThread implements Runnable {
        public CapThread() {
            new Thread(this).start();
        }
        @Override
        public void run() {
            try {
                captor = JpcapCaptor.openDevice(currentDevice, 65535, false, 20);
                captor.setFilter("ip", true);
                statusLabel.setText("Running...");
                captor.loopPacket(-1, new Receiver());
                statusLabel.setText("Done. PacketNumbers:" + packageCounter);
                printWriter.close();
                dataOutput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
	
    public static void main(String[] args) {
        new Capturer();
    }
}