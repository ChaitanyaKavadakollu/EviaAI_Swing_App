public class TestSaveItem {
    public static void main(String[] args) {
        EviaAIDAO dao = new EviaAIDAO();
        // create a test user and save
        User u = new User("testuser@example.com", "pass");
        if (!dao.saveUser(u)) {
            System.err.println("saveUser failed: " + EviaAIDAO.getLastError());
            return;
        }
        System.out.println("saveUser OK");

        // create item
        ItemInfo item = new ItemInfo("test-item",
                "wood, nails",
                "hammer pieces together",
                "use to prop door",
                "home",
                "testuser@example.com");

        if (!dao.saveItem(item)) {
            System.err.println("saveItem failed: " + EviaAIDAO.getLastError());
            return;
        }
        System.out.println("saveItem OK");

        ItemInfo read = dao.getItem("test-item");
        if (read == null) {
            System.err.println("getItem returned null");
        } else {
            System.out.println("getItem: " + read.name + ", contributor=" + read.contributor + ", rawMaterials=" + read.rawMaterials);
        }
    }
}
