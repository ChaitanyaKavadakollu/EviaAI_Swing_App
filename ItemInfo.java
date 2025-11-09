public class ItemInfo {
    public String name;
    public String rawMaterials;
    public String howToMake;
    public String howToUse;
    public String whereToUse;
    public String contributor;
    public int credits;

    public ItemInfo(String name, String rawMaterials, String howToMake, 
                   String howToUse, String whereToUse, String contributor) {
        this.name = name;
        this.rawMaterials = rawMaterials;
        this.howToMake = howToMake;
        this.howToUse = howToUse;
        this.whereToUse = whereToUse;
        this.contributor = contributor;
        this.credits = 0;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRawMaterials() { return rawMaterials; }
    public void setRawMaterials(String rawMaterials) { this.rawMaterials = rawMaterials; }

    public String getHowToMake() { return howToMake; }
    public void setHowToMake(String howToMake) { this.howToMake = howToMake; }

    public String getHowToUse() { return howToUse; }
    public void setHowToUse(String howToUse) { this.howToUse = howToUse; }

    public String getWhereToUse() { return whereToUse; }
    public void setWhereToUse(String whereToUse) { this.whereToUse = whereToUse; }

    public String getContributor() { return contributor; }
    public void setContributor(String contributor) { this.contributor = contributor; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits + 10; }
}