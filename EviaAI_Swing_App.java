import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class EviaAI_Swing_App extends JFrame {

    private final EviaAIDAO dao = new EviaAIDAO();
    private User currentUser = null;
    private final JTextField searchField = new JTextField(26);
    private final JButton searchBtn = new JButton("Search");
    private final JTextArea resultArea = new JTextArea(14, 48);
    private final JLabel userLabel = new JLabel("Not logged in");
    private final JButton loginBtn = new JButton("Login / Register");
    private final JButton logoutBtn = new JButton("Logout");
    private final JButton contributeBtn = new JButton("Contribute Info");
    private final JButton editBtn = new JButton("Edit Info");
    private final JButton rankingsBtn = new JButton("Rankings");
    private JPanel center;
    private JPanel footer;

    public EviaAI_Swing_App() {
        setTitle("Evia.AI - Knowledge Contribution Platform");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));
        getContentPane().setBackground(new Color(240, 248, 255));

        setupUI();
        setupListeners();
        
        center.setVisible(false);
        footer.setVisible(false);

        setPreferredSize(new Dimension(900, 560));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        SwingUtilities.invokeLater(this::showLoginDialog);
    }

    private void setupUI() {
        // Header setup
        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setBackground(new Color(200, 220, 240));
        
        JLabel title = new JLabel("Evia.AI");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        JLabel subtitle = new JLabel("Search, contribute and learn how to make everyday items");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        JPanel titleBox = new JPanel(new GridLayout(0, 1));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        headerRight.setOpaque(false);
        headerRight.add(userLabel);
        headerRight.add(loginBtn);
        headerRight.add(logoutBtn);
        logoutBtn.setVisible(false);
        header.add(headerRight, BorderLayout.EAST);
        
        add(header, BorderLayout.NORTH);

        // Center setup
        center = new JPanel(new BorderLayout(10, 10));
        center.setBackground(new Color(245, 250, 255));
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        searchPanel.setBackground(new Color(230, 240, 250));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Item"));
        searchPanel.add(new JLabel("Item:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        center.add(searchPanel, BorderLayout.NORTH);

        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setBackground(new Color(255, 255, 240));
        JScrollPane scroll = new JScrollPane(resultArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Item Details"));
        center.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actions.setBackground(new Color(230, 240, 250));
        actions.add(contributeBtn);
        actions.add(editBtn);
        actions.add(rankingsBtn);
        center.add(actions, BorderLayout.SOUTH);
        
        add(center, BorderLayout.CENTER);

        // Footer setup
        footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(200, 220, 240));
        footer.add(new JLabel("Contribute accurate info. Correct contributions earn +10 credits."), BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        searchBtn.addActionListener(e -> onSearch());
        loginBtn.addActionListener(e -> showLoginDialog());
        logoutBtn.addActionListener(e -> doLogout());
        contributeBtn.addActionListener(e -> onContribute());
        editBtn.addActionListener(e -> onEdit());
        rankingsBtn.addActionListener(e -> showRankingsWindow());
    }

    private void showLoginDialog() {
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
        JTextField idf = new JTextField();
        JPasswordField passf = new JPasswordField();
        p.add(new JLabel("Email or Phone:"));
        p.add(idf);
        p.add(new JLabel("Password:"));
        p.add(passf);

        Object[] options = {"Login", "Register", "Cancel"};
        int opt = JOptionPane.showOptionDialog(this, p, "Login/Register",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        if (opt == JOptionPane.YES_OPTION) {
            String id = idf.getText().trim();
            String pwd = new String(passf.getPassword());
            User u = dao.getUser(id);
            if (u != null && u.password.equals(pwd)) {
                currentUser = u;
                userLabel.setText("Logged in: " + id + " (" + u.credits + " credits)");
                loginBtn.setVisible(false);
                logoutBtn.setVisible(true);
                center.setVisible(true);
                footer.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials");
            }
        } else if (opt == JOptionPane.NO_OPTION) {
            String id = idf.getText().trim();
            String pwd = new String(passf.getPassword());
            if (dao.getUser(id) != null) {
                JOptionPane.showMessageDialog(this, "User exists.");
                return;
            }
            User u = new User(id, pwd);
            boolean ok = dao.saveUser(u);
            if (!ok) {
                String err = EviaAIDAO.getLastError();
                String msg = "Failed to create user in database.\n" + (err != null ? err.substring(0, Math.min(400, err.length())) + (err.length() > 400 ? "\n(see logs for full details)" : "") : "(no details)");
                JOptionPane.showMessageDialog(this, msg);
                return;
            }
            currentUser = u;
            userLabel.setText("Logged in: " + id + " (0 credits)");
            loginBtn.setVisible(false);
            logoutBtn.setVisible(true);
            center.setVisible(true);
            footer.setVisible(true);
        }
    }

    private void doLogout() {
        currentUser = null;
        userLabel.setText("Not logged in");
        loginBtn.setVisible(true);
        logoutBtn.setVisible(false);
        center.setVisible(false);
        footer.setVisible(false);
        resultArea.setText("");
    }

    private void onSearch() {
        String q = searchField.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Type an item");
            return;
        }
        
        ItemInfo it = dao.getItem(q);
        if (it == null) {
            resultArea.setText("Item not found. Contribute info to earn credits.");
        } else {
            resultArea.setText(
                "Item: " + it.name +
                "\n\nRaw materials: " + it.rawMaterials +
                "\n\nHow to make: " + it.howToMake +
                "\n\nHow to use: " + it.howToUse +
                "\n\nWhere to use: " + it.whereToUse +
                "\n\nContributor: " + it.contributor +
                "\nCredits: " + it.credits
            );
        }
    }

    private void onContribute() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please login first");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));
        JTextField nameField = new JTextField();
        JTextArea materialsArea = new JTextArea(3, 20);
        JTextArea makeArea = new JTextArea(3, 20);
        JTextArea useArea = new JTextArea(3, 20);
        JTextArea whereArea = new JTextArea(3, 20);

        materialsArea.setLineWrap(true);
        makeArea.setLineWrap(true);
        useArea.setLineWrap(true);
        whereArea.setLineWrap(true);

        panel.add(new JLabel("Item Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Raw Materials:"));
        panel.add(new JScrollPane(materialsArea));
        panel.add(new JLabel("How to Make:"));
        panel.add(new JScrollPane(makeArea));
        panel.add(new JLabel("How to Use:"));
        panel.add(new JScrollPane(useArea));
        panel.add(new JLabel("Where to Use:"));
        panel.add(new JScrollPane(whereArea));

        int result = JOptionPane.showConfirmDialog(this, panel, "Contribute Item Info",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim().toLowerCase();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Item name is required");
                return;
            }

            ItemInfo item = new ItemInfo(
                name,
                materialsArea.getText().trim(),
                makeArea.getText().trim(),
                useArea.getText().trim(),
                whereArea.getText().trim(),
                currentUser.id
            );

            boolean ok = dao.saveItem(item);
            if (ok) {
                currentUser.contributedItems.add(name);
                JOptionPane.showMessageDialog(this, "Information contributed successfully!");
            } else {
                String err = EviaAIDAO.getLastError();
                String msg = "Failed to save item to database.\n" + (err != null ? err.substring(0, Math.min(400, err.length())) + (err.length() > 400 ? "\n(see logs for full details)" : "") : "(no details)");
                String dumpFile = EviaAIDAO.dumpLastErrorToFile();
                if (dumpFile != null) {
                    msg += "\nFull log saved to: " + dumpFile;
                } else {
                    msg += "\nFailed to write full log to file.";
                }
                JOptionPane.showMessageDialog(this, msg);
            }
        }
    }

    private void onEdit() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please login first");
            return;
        }

        String itemName = JOptionPane.showInputDialog(this, "Enter item name to edit:");
        if (itemName == null || itemName.trim().isEmpty()) return;

        itemName = itemName.trim().toLowerCase();
        ItemInfo item = dao.getItem(itemName);

        if (item == null) {
            JOptionPane.showMessageDialog(this, "Item not found");
            return;
        }

        if (!item.contributor.equals(currentUser.id)) {
            JOptionPane.showMessageDialog(this, "You can only edit items you contributed");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));
        JTextArea materialsArea = new JTextArea(item.rawMaterials, 3, 20);
        JTextArea makeArea = new JTextArea(item.howToMake, 3, 20);
        JTextArea useArea = new JTextArea(item.howToUse, 3, 20);
        JTextArea whereArea = new JTextArea(item.whereToUse, 3, 20);

        materialsArea.setLineWrap(true);
        makeArea.setLineWrap(true);
        useArea.setLineWrap(true);
        whereArea.setLineWrap(true);

        panel.add(new JLabel("Raw Materials:"));
        panel.add(new JScrollPane(materialsArea));
        panel.add(new JLabel("How to Make:"));
        panel.add(new JScrollPane(makeArea));
        panel.add(new JLabel("How to Use:"));
        panel.add(new JScrollPane(useArea));
        panel.add(new JLabel("Where to Use:"));
        panel.add(new JScrollPane(whereArea));

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Item: " + itemName,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            item.rawMaterials = materialsArea.getText().trim();
            item.howToMake = makeArea.getText().trim();
            item.howToUse = useArea.getText().trim();
            item.whereToUse = whereArea.getText().trim();

            dao.saveItem(item);
            JOptionPane.showMessageDialog(this, "Item updated successfully!");
        }
    }

    private void showRankingsWindow() {
    java.util.List<User> topUsers = dao.getTopUsers();
        if (topUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No rankings available yet");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Top Contributors:\n\n");
        int rank = 1;
        for (User u : topUsers) {
            sb.append(rank++).append(". ")
              .append(u.id)
              .append(" (").append(u.credits).append(" credits)")
              .append(" - Items: ").append(u.contributedItems.size())
              .append("\n");
        }

        JTextArea rankingArea = new JTextArea(sb.toString());
        rankingArea.setEditable(false);
        rankingArea.setLineWrap(true);
        rankingArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(rankingArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Rankings",
                JOptionPane.PLAIN_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(EviaAI_Swing_App::new);
    }
}

// User model moved into main file per request. Non-public top-level class so other
// classes in the package can still reference it.
class User {
    public String id;
    public String password;
    public int credits;
    public java.util.Set<String> contributedItems = new java.util.HashSet<>();

    public User(String id, String password) {
        this.id = id;
        this.password = password;
        this.credits = 0;
    }

    // Convenience getters for consumers that expect bean-style API
    public String getId() { return id; }
    public String getPassword() { return password; }
    public int getCredits() { return credits; }
    public java.util.Set<String> getContributedItems() { return java.util.Collections.unmodifiableSet(contributedItems); }

    public void setPassword(String password) { this.password = password; }
    public void setCredits(int credits) { this.credits = credits; }

    public void addContributedItem(String name) { this.contributedItems.add(name); }
}