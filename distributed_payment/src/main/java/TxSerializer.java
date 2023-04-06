
import org.apache.kafka.common.serialization.Serializer;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;

public class TxSerializer implements Serializer<Transaction> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        //nothing to configure
    }

    @Override
    public  byte[] serialize(String topic, Transaction data) {
        int sizeOfInBank;
        int sizeOfOutBank;
        int sizeOfAmount;
        int sizeOfSerialNumber;
        int sizeOfInBankPartition;
        int sizeOfOutBankPartition;
        int sizeOfCategory;
        byte[] serializedInBank;
        byte[] serializedOutBank;
        byte[] serializedSerialNumber;
        byte[] serializedInBankPartition;
        byte[] serializedOutBankPartition;
        byte[] serializedAmount;
        byte[] serializedCategory;

        try {
            if (data == null)
                return null;
            String encoding = "UTF8";
            serializedInBank = data.getInBank().getBytes(encoding);
            sizeOfInBank = serializedInBank.length;
            serializedOutBank = data.getOutBank().getBytes(encoding);
            sizeOfOutBank = serializedOutBank.length;
            serializedAmount = ByteBuffer.allocate(Long.BYTES).putLong(data.getAmount()).array();
            sizeOfAmount = serializedAmount.length;
            serializedSerialNumber = ByteBuffer.allocate(Long.BYTES).putLong(data.getSerialNumber()).array();
            sizeOfSerialNumber = serializedSerialNumber.length;
            serializedInBankPartition = ByteBuffer.allocate(Integer.BYTES).putInt(data.getInBankPartition()).array();;
            sizeOfInBankPartition = serializedInBankPartition.length;
            serializedOutBankPartition = ByteBuffer.allocate(Integer.BYTES).putInt(data.getOutBankPartition()).array();;
            sizeOfOutBankPartition = serializedOutBankPartition.length;
            serializedCategory = ByteBuffer.allocate(Integer.BYTES).putInt(data.getCategory()).array();;
            sizeOfCategory = serializedCategory.length;
            ByteBuffer buf = ByteBuffer.allocate(4+4+sizeOfInBank+4+sizeOfOutBank+4+sizeOfAmount+4+sizeOfCategory+4+sizeOfSerialNumber+4+sizeOfInBankPartition+4+sizeOfOutBankPartition);
            buf.putInt(sizeOfInBank);
            buf.put(serializedInBank);
            buf.putInt(sizeOfOutBank);
            buf.put(serializedOutBank);
            buf.putInt(sizeOfAmount);
            buf.put(serializedAmount);
            buf.putInt(sizeOfSerialNumber);
            buf.put(serializedSerialNumber);
            buf.putInt(sizeOfInBankPartition);
            buf.put(serializedInBankPartition);
            buf.putInt(sizeOfOutBankPartition);
            buf.put(serializedOutBankPartition);
            buf.putInt(sizeOfCategory);
            buf.put(serializedCategory);

            return buf.array();
           } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void close() {
        //nothing to do
    }
}

