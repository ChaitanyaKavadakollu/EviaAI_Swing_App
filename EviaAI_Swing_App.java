// EviaAI_Swing_App - with demo items
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class EviaAI_Swing_App extends JFrame {
    static class ItemInfo {
        String name;
        String rawMaterials;
        String howToMake;
        String howToUse;
        String whereToUse;
        String contributor;
        int credits;
        ItemInfo(String name, String rawMaterials, String howToMake, String howToUse, String whereToUse, String contributor) {
            this.name = name; this.rawMaterials = rawMaterials; this.howToMake = howToMake;
            this.howToUse = howToUse; this.whereToUse = whereToUse; this.contributor = contributor;
            this.credits = 0;
        }
    }
    static class User {
        String id, password; int credits; Set<String> contributedItems = new HashSet<>();
        User(String id, String password) { this.id=id; this.password=password; this.credits=0; }
    }

    private final Map<String, ItemInfo> items = new HashMap<>();
    private final Map<String, User> users = new HashMap<>();
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

        JPanel header = new JPanel(new BorderLayout(8,8));
        header.setBackground(new Color(200, 220, 240));
        JLabel title = new JLabel("Evia.AI");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        JLabel subtitle = new JLabel("Search, contribute and learn how to make everyday items");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JPanel titleBox = new JPanel(new GridLayout(0,1));
        titleBox.setOpaque(false);
        titleBox.add(title); titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        headerRight.setOpaque(false);
        headerRight.add(userLabel); headerRight.add(loginBtn); headerRight.add(logoutBtn);
        logoutBtn.setVisible(false);
        header.add(headerRight, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        center = new JPanel(new BorderLayout(10,10));
        center.setBackground(new Color(245, 250, 255));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        searchPanel.setBackground(new Color(230, 240, 250));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Item"));
        searchPanel.add(new JLabel("Item:")); searchPanel.add(searchField); searchPanel.add(searchBtn);
        center.add(searchPanel, BorderLayout.NORTH);

        resultArea.setEditable(false); resultArea.setLineWrap(true); resultArea.setWrapStyleWord(true);
        resultArea.setBackground(new Color(255,255,240));
        JScrollPane scroll = new JScrollPane(resultArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Item Details"));
        center.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));
        actions.setBackground(new Color(230,240,250));
        actions.add(contributeBtn); actions.add(editBtn); actions.add(rankingsBtn);
        center.add(actions, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        footer = new JPanel(new BorderLayout()); footer.setBackground(new Color(200,220,240));
        footer.add(new JLabel("Contribute accurate info. Correct contributions earn +10 credits."), BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> onSearch());
        loginBtn.addActionListener(e -> showLoginDialog());
        logoutBtn.addActionListener(e -> doLogout());

        center.setVisible(false); footer.setVisible(false);

        seedDemoData();
        setPreferredSize(new Dimension(900,560));
        pack(); setLocationRelativeTo(null); setVisible(true);
        SwingUtilities.invokeLater(this::showLoginDialog);
    }

    private void seedDemoData() {
        User admin = new User("admin@evia.ai","admin123"); admin.credits=500; users.put(admin.id,admin);
        items.put("biryani", new ItemInfo("biryani","rice, meat, spices, onions, ghee","Cook rice and meat with spices in layers","Eat with raita","Restaurants, homes", admin.id));
        items.put("pen", new ItemInfo("pen","plastic body, ink, nib","Assemble plastic body with ink refill","Write on paper","Offices, schools", admin.id));
        items.put("water bottle", new ItemInfo("water bottle","plastic or steel, cap","Mold body and attach cap","Fill with water to drink","Homes, outdoors, offices", admin.id));
        items.put("pillow", new ItemInfo("pillow","cotton/foam, fabric cover","Fill fabric cover with cotton or foam","Rest your head on it","Bedroom, sofa", admin.id));
        items.put("smart phone", new ItemInfo("smart phone","glass, screen, processor, battery, case","Assemble screen, chip, battery, install OS","Make calls, browse, apps","Everywhere", admin.id));
    }

    private void showLoginDialog() {
        JPanel p = new JPanel(new GridLayout(0,1,4,4));
        JTextField idf=new JTextField(); JPasswordField passf=new JPasswordField();
        p.add(new JLabel("Email or Phone:")); p.add(idf);
        p.add(new JLabel("Password:")); p.add(passf);
        Object[] options={"Login","Register","Cancel"};
        int opt=JOptionPane.showOptionDialog(this,p,"Login/Register",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null,options,options[0]);
        if(opt==JOptionPane.YES_OPTION){
            String id=idf.getText().trim(); String pwd=new String(passf.getPassword());
            User u=users.get(id);
            if(u!=null && u.password.equals(pwd)){
                currentUser=u; userLabel.setText("Logged in: "+id+" ("+u.credits+" credits)");
                loginBtn.setVisible(false); logoutBtn.setVisible(true);
                center.setVisible(true); footer.setVisible(true);
            } else JOptionPane.showMessageDialog(this,"Invalid credentials");
        } else if(opt==JOptionPane.NO_OPTION){
            String id=idf.getText().trim(); String pwd=new String(passf.getPassword());
            if(users.containsKey(id)){JOptionPane.showMessageDialog(this,"User exists.");return;}
            User u=new User(id,pwd); users.put(id,u); currentUser=u;
            userLabel.setText("Logged in: "+id+" (0 credits)");
            loginBtn.setVisible(false); logoutBtn.setVisible(true);
            center.setVisible(true); footer.setVisible(true);
        }
    }

    private void doLogout(){
        currentUser=null; userLabel.setText("Not logged in");
        loginBtn.setVisible(true); logoutBtn.setVisible(false);
        center.setVisible(false); footer.setVisible(false);
        resultArea.setText("");
    }

    private void onSearch(){
        String q=searchField.getText().trim().toLowerCase();
        if(q.isEmpty()){JOptionPane.showMessageDialog(this,"Type an item"); return;}
        ItemInfo it=items.get(q);
        if(it==null){resultArea.setText("Item not found. Contribute info to earn credits.");}
        else{
            resultArea.setText("Item: "+it.name+"\n\nRaw materials: "+it.rawMaterials+"\n\nHow to make: "+it.howToMake+"\n\nHow to use: "+it.howToUse+"\n\nWhere to use: "+it.whereToUse+"\n\nContributor: "+it.contributor+"\nCredits: "+it.credits);
        }
    }

    private void onContribute(){}
    private void onEdit(){}
    private void showRankingsWindow(){}

    public static void main(String[] a){try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){} SwingUtilities.invokeLater(EviaAI_Swing_App::new);} 
}