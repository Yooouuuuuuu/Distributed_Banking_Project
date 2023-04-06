public class Transaction {
    private String inBank;
    private String outBank;
    private long amount;
    private long serialNumber;
    private int inBankPartition;
    private int outBankPartition;
    private int category;
    //private long number;


    public Transaction() {}

    public Transaction(String inBank, String outBank, long amount, long serialNumber, int inBankPartition, int outBankPartition, int category) {
        setInBank(inBank);
        setOutBank(outBank);
        setAmount(amount);
        setSerialNumber(serialNumber);
        setInBankPartition(inBankPartition);
        setOutBankPartition(outBankPartition);
        setCategory(category);
        //setNumber(number);
    }
    public void setInBank(String inBank){
        this.inBank = inBank;
    }
    public void setOutBank(String outBank){
        this.outBank = outBank;
    }
    public void setAmount(long amount){
        this.amount = amount;
    }
    public void setSerialNumber(long serialNumber){
        this.serialNumber = serialNumber;
    }
    public void setInBankPartition(int inBankPartition){
        this.inBankPartition = inBankPartition;
    }
    public void setOutBankPartition(int outBankPartition){
        this.outBankPartition = outBankPartition;
    }
    public void setCategory(int category){
        this.category = category;
    }
    //public void setNumber(long number){        this.number = number;    }


    public String getInBank()
    {
        return inBank;
    }
    public String getOutBank()
    {
        return outBank;
    }
    public long getAmount()
    {
        return amount;
    }
    public long getSerialNumber()
    {
        return serialNumber;
    }
    public int getInBankPartition()
    {
        return inBankPartition;
    }
    public int getOutBankPartition()
    {
        return outBankPartition;
    }
    public int getCategory()
    {
        return category;
    }
    //public long getNumber(){        return number;    }
}


