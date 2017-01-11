package jem.crawling;

import pw.phylame.jem.crawler.ProviderManager;
import pw.phylame.jem.epm.EpmManager;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static pw.phylame.ycl.util.CollectionUtils.propertiesFor;

public class Board implements ActionListener {
    private static final String TAG = "Board";

    JPanel root;
    private JTextArea taConsole;
    private JComboBox<Item> cbFormat;
    private JButton btnDownload;
    private JCheckBox cbBackup;
    private JTextField tfOutput;
    private JTextField tfUrl;
    private JButton btnAbout;
    private JButton btnOutput;
    private JButton btnSearch;
    private JButton btnClean;
    private JButton btnMore;

    void init() {
        int index = 0, selected = -1;
        for (String name : EpmManager.supportedMakers()) {
            cbFormat.addItem(new Item(name));
            if (name.equals("epub")) {
                selected = index;
            }
            ++index;
        }
        cbFormat.setSelectedIndex(selected);
        btnSearch.addActionListener(this);
        btnOutput.addActionListener(this);
        btnAbout.addActionListener(this);
        btnClean.addActionListener(this);
        btnDownload.addActionListener(this);
        btnMore.addActionListener(this);
        tfUrl.addActionListener(this);
        tfOutput.addActionListener(this);
        tfOutput.setText(System.getProperty("user.dir"));
    }

    void print(String text) {
        taConsole.append(text);
        taConsole.setCaretPosition(taConsole.getDocument().getLength());
    }

    void setStartIcon() {
        btnDownload.setToolTipText("开始下载电子书");
        try {
            btnDownload.setIcon(new ImageIcon(IOUtils.resourceFor("!jem/crawling/start.png")));
        } catch (MalformedURLException e) {
            Log.e(TAG, e);
            btnDownload.setText("开始");
        }
    }

    void setStopIcon() {
        btnDownload.setToolTipText("停止操作");
        try {
            btnDownload.setIcon(new ImageIcon(IOUtils.resourceFor("!jem/crawling/stop.png")));
        } catch (MalformedURLException e) {
            Log.e(TAG, e);
            btnDownload.setText("停止");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnDownload || src == tfUrl || src == tfOutput) {
            downloadFile();
        } else if (src == btnClean) {
            taConsole.setText(null);
        } else if (src == btnAbout) {
            aboutApp();
        } else if (src == btnOutput) {
            selectFile();
        } else if (src == btnSearch) {
            searchBook();
        } else if (src == btnMore) {
            String path = tfOutput.getText();
            if (StringUtils.isNotBlank(path)) {
                try {
                    Desktop.getDesktop().browse(new File(path).toURI());
                } catch (IOException ex) {
                    Log.e(TAG, ex);
                }
            }
        }
    }

    private void downloadFile() {
        Crawling app = Crawling.INSTANCE;
        if (app.stop()) {
            app.echo("已经取消操作\n");
            setStartIcon();
            return;
        }
        String url = tfUrl.getText().trim();
        if (url.isEmpty()) {
            note("下载电子书", "请输入链接地址", JOptionPane.ERROR_MESSAGE);
            tfUrl.requestFocus();
            return;
        }
        String path = tfOutput.getText();
        if (StringUtils.isBlank(path)) {
            note("下载电子书", "请选择保存路径", JOptionPane.ERROR_MESSAGE);
            tfOutput.requestFocus();
            return;
        }
        app.fetchBook(url, path, ((Item) cbFormat.getSelectedItem()).name, cbBackup.isSelected());
        setStopIcon();
    }

    private JFileChooser fileChooser = new JFileChooser();

    private void selectFile() {
        fileChooser.setDialogTitle("选择保存位置");
        String path = tfOutput.getText();
        if (StringUtils.isNotBlank(path)) {
            if (path.matches("[\\w]:")) { // windows driver
                path += "\\";
            }
            File file = new File(path);
            File dir = !file.isDirectory() ? file.getParentFile() : file;
            if (dir != null && dir.exists()) {
                fileChooser.setCurrentDirectory(dir);
                if (dir != file) {
                    fileChooser.setSelectedFile(file);
                }
            }
        }
        if (fileChooser.showSaveDialog(Crawling.INSTANCE.getForm()) == JFileChooser.APPROVE_OPTION) {
            tfOutput.setText(fileChooser.getSelectedFile().getPath());
        }
    }

    private void searchBook() {
        note("搜索电子书", "功能正在开发中:)", JOptionPane.INFORMATION_MESSAGE);
    }

    private void aboutApp() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        for (final String host : ProviderManager.knownHosts()) {
            JLabel label = new JLabel(String.format("<html>&nbsp;<a href='%s'>%s</a></html>", host, host));
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        Desktop.getDesktop().browse(new URI(host));
                    } catch (IOException | URISyntaxException ignored) {
                    }
                }
            });
            panel.add(label);
        }
        Object[] message = {"支持的网址：", panel};
        note("PW Crawling", message, JOptionPane.PLAIN_MESSAGE);
    }

    public void note(String title, Object message, int type) {
        JOptionPane.showMessageDialog(Crawling.INSTANCE.getForm(), message, title, type);
    }


    private static class Item {
        private static Properties prop;

        static {
            try {
                prop = propertiesFor("!jem/crawling/format-names.properties");
            } catch (IOException e) {
                prop = new Properties();
            }
        }

        final String name;
        String message;

        Item(String name) {
            this.name = name;
            message = prop.getProperty(name);
            if (message == null) {
                message = name.toUpperCase();
            }
        }

        @Override
        public String toString() {
            return message;
        }
    }
}
