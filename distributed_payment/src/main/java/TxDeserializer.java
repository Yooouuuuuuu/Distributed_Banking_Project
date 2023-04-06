
import org.apache.kafka.common.serialization.Deserializer;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;

public class TxDeserializer implements Deserializer<Transaction> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        //nothing to configure
    }

    @Override
    public  Transaction deserialize(String topic, byte[] data) {

        try {
            if (data == null) {
                System.out.println("Null received while deserialize.");
                return null;
            }
            ByteBuffer buf = ByteBuffer.wrap(data);

            int sizeOfInBank = buf.getInt();
            byte[] inBankBytes = new byte[sizeOfInBank];
            buf.get(inBankBytes);
            String encoding = "UTF8";
            String deserializedInBank = new String(inBankBytes, encoding);

            int sizeOfOutBank = buf.getInt();
            byte[] outBankBytes = new byte[sizeOfOutBank];
            buf.get(outBankBytes);
            String deserializedOutBank = new String(outBankBytes, encoding);

            int sizeOfAmount = buf.getInt();
            byte[] amountBytes = new byte[sizeOfAmount];
            buf.get(amountBytes);
            long deserializedAmount = ByteBuffer.wrap(amountBytes).getLong();

            int sizeOfSerialNumber = buf.getInt();
            byte[] serialNumberBytes = new byte[sizeOfSerialNumber];
            buf.get(serialNumberBytes);
            long deserializedSerialNumber = ByteBuffer.wrap(serialNumberBytes).getLong();

            int sizeOfInBankPartition = buf.getInt();
            byte[] inBankPartitionBytes = new byte[sizeOfInBankPartition];
            buf.get(inBankPartitionBytes);
            int deserializedInBankPartition = ByteBuffer.wrap(inBankPartitionBytes).getInt();

            int sizeOfOutBankPartition = buf.getInt();
            byte[] outBankPartitionBytes = new byte[sizeOfOutBankPartition];
            buf.get(outBankPartitionBytes);
            int deserializedOutBankPartition = ByteBuffer.wrap(outBankPartitionBytes).getInt();

            int sizeOfCategory = buf.getInt();
            byte[] categoryBytes = new byte[sizeOfCategory];
            buf.get(categoryBytes);
            int deserializedCategory = ByteBuffer.wrap(categoryBytes).getInt();

            return new Transaction(deserializedInBank, deserializedOutBank, deserializedAmount,
                    deserializedSerialNumber, deserializedInBankPartition, deserializedOutBankPartition, deserializedCategory);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void close() {
        //nothing to do
    }
}

