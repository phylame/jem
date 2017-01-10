package jem.crawling;

import org.jetbrains.annotations.NotNull;
import pw.phylame.jem.epm.EpmManager;
import pw.phylame.ycl.util.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;

import static pw.phylame.ycl.util.CollectionUtils.propertiesFor;

public class Board implements ActionListener {
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
        tfUrl.addActionListener(this);
        tfOutput.setText(System.getProperty("user.dir"));
    }

    void redirectOutput() {
        PrintStream ps = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
            }

            @Override
            public void write(@NotNull byte[] b, int off, int len) throws IOException {
                taConsole.append(new String(b, off, len));
                taConsole.setCaretPosition(taConsole.getDocument().getLength());
            }
        });
        System.setOut(ps);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnDownload || src == tfUrl) {
            downloadFile();
        } else if (src == btnClean) {
            taConsole.setText(null);
        } else if (src == btnAbout) {
            aboutApp();
        } else if (src == btnOutput) {
            selectFile();
        } else if (src == btnSearch) {
            searchBook();
        }
    }

    private void downloadFile() {
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
        String format = ((Item) cbFormat.getSelectedItem()).name;
        fetchBook(url, path, format);
    }

    private void fetchBook(String url, String output, String format) {
        Crawling.INSTANCE.fetchBook(url, output, format);
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

    }

    private void note(String title, Object message, int type) {
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
