/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package my.avroSchema;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;

@org.apache.avro.specific.AvroGenerated
public class Transaction extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 1335886478144484984L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Transaction\",\"namespace\":\"my.avroSchema\",\"fields\":[{\"name\":\"serialNumber\",\"type\":\"long\"},{\"name\":\"outbank\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"outAccount\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"inbank\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"inAccount\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"outbankPartition\",\"type\":\"int\"},{\"name\":\"inbankPartition\",\"type\":\"int\"},{\"name\":\"amount\",\"type\":\"long\"},{\"name\":\"category\",\"type\":\"int\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();

  private static final BinaryMessageEncoder<Transaction> ENCODER =
      new BinaryMessageEncoder<>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<Transaction> DECODER =
      new BinaryMessageDecoder<>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<Transaction> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<Transaction> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<Transaction> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this Transaction to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a Transaction from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a Transaction instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static Transaction fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  private long serialNumber;
  private java.lang.String outbank;
  private java.lang.String outAccount;
  private java.lang.String inbank;
  private java.lang.String inAccount;
  private int outbankPartition;
  private int inbankPartition;
  private long amount;
  private int category;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public Transaction() {}

  /**
   * All-args constructor.
   * @param serialNumber The new value for serialNumber
   * @param outbank The new value for outbank
   * @param outAccount The new value for outAccount
   * @param inbank The new value for inbank
   * @param inAccount The new value for inAccount
   * @param outbankPartition The new value for outbankPartition
   * @param inbankPartition The new value for inbankPartition
   * @param amount The new value for amount
   * @param category The new value for category
   */
  public Transaction(java.lang.Long serialNumber, java.lang.String outbank, java.lang.String outAccount, java.lang.String inbank, java.lang.String inAccount, java.lang.Integer outbankPartition, java.lang.Integer inbankPartition, java.lang.Long amount, java.lang.Integer category) {
    this.serialNumber = serialNumber;
    this.outbank = outbank;
    this.outAccount = outAccount;
    this.inbank = inbank;
    this.inAccount = inAccount;
    this.outbankPartition = outbankPartition;
    this.inbankPartition = inbankPartition;
    this.amount = amount;
    this.category = category;
  }

  @Override
  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }

  @Override
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }

  // Used by DatumWriter.  Applications should not call.
  @Override
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return serialNumber;
    case 1: return outbank;
    case 2: return outAccount;
    case 3: return inbank;
    case 4: return inAccount;
    case 5: return outbankPartition;
    case 6: return inbankPartition;
    case 7: return amount;
    case 8: return category;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  // Used by DatumReader.  Applications should not call.
  @Override
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: serialNumber = (java.lang.Long)value$; break;
    case 1: outbank = value$ != null ? value$.toString() : null; break;
    case 2: outAccount = value$ != null ? value$.toString() : null; break;
    case 3: inbank = value$ != null ? value$.toString() : null; break;
    case 4: inAccount = value$ != null ? value$.toString() : null; break;
    case 5: outbankPartition = (java.lang.Integer)value$; break;
    case 6: inbankPartition = (java.lang.Integer)value$; break;
    case 7: amount = (java.lang.Long)value$; break;
    case 8: category = (java.lang.Integer)value$; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'serialNumber' field.
   * @return The value of the 'serialNumber' field.
   */
  public long getSerialNumber() {
    return serialNumber;
  }



  /**
   * Gets the value of the 'outbank' field.
   * @return The value of the 'outbank' field.
   */
  public java.lang.String getOutbank() {
    return outbank;
  }



  /**
   * Gets the value of the 'outAccount' field.
   * @return The value of the 'outAccount' field.
   */
  public java.lang.String getOutAccount() {
    return outAccount;
  }



  /**
   * Gets the value of the 'inbank' field.
   * @return The value of the 'inbank' field.
   */
  public java.lang.String getInbank() {
    return inbank;
  }



  /**
   * Gets the value of the 'inAccount' field.
   * @return The value of the 'inAccount' field.
   */
  public java.lang.String getInAccount() {
    return inAccount;
  }



  /**
   * Gets the value of the 'outbankPartition' field.
   * @return The value of the 'outbankPartition' field.
   */
  public int getOutbankPartition() {
    return outbankPartition;
  }



  /**
   * Gets the value of the 'inbankPartition' field.
   * @return The value of the 'inbankPartition' field.
   */
  public int getInbankPartition() {
    return inbankPartition;
  }



  /**
   * Gets the value of the 'amount' field.
   * @return The value of the 'amount' field.
   */
  public long getAmount() {
    return amount;
  }



  /**
   * Gets the value of the 'category' field.
   * @return The value of the 'category' field.
   */
  public int getCategory() {
    return category;
  }



  /**
   * Creates a new Transaction RecordBuilder.
   * @return A new Transaction RecordBuilder
   */
  public static my.avroSchema.Transaction.Builder newBuilder() {
    return new my.avroSchema.Transaction.Builder();
  }

  /**
   * Creates a new Transaction RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new Transaction RecordBuilder
   */
  public static my.avroSchema.Transaction.Builder newBuilder(my.avroSchema.Transaction.Builder other) {
    if (other == null) {
      return new my.avroSchema.Transaction.Builder();
    } else {
      return new my.avroSchema.Transaction.Builder(other);
    }
  }

  /**
   * Creates a new Transaction RecordBuilder by copying an existing Transaction instance.
   * @param other The existing instance to copy.
   * @return A new Transaction RecordBuilder
   */
  public static my.avroSchema.Transaction.Builder newBuilder(my.avroSchema.Transaction other) {
    if (other == null) {
      return new my.avroSchema.Transaction.Builder();
    } else {
      return new my.avroSchema.Transaction.Builder(other);
    }
  }

  /**
   * RecordBuilder for Transaction instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Transaction>
    implements org.apache.avro.data.RecordBuilder<Transaction> {

    private long serialNumber;
    private java.lang.String outbank;
    private java.lang.String outAccount;
    private java.lang.String inbank;
    private java.lang.String inAccount;
    private int outbankPartition;
    private int inbankPartition;
    private long amount;
    private int category;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$, MODEL$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(my.avroSchema.Transaction.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.serialNumber)) {
        this.serialNumber = data().deepCopy(fields()[0].schema(), other.serialNumber);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.outbank)) {
        this.outbank = data().deepCopy(fields()[1].schema(), other.outbank);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
      if (isValidValue(fields()[2], other.outAccount)) {
        this.outAccount = data().deepCopy(fields()[2].schema(), other.outAccount);
        fieldSetFlags()[2] = other.fieldSetFlags()[2];
      }
      if (isValidValue(fields()[3], other.inbank)) {
        this.inbank = data().deepCopy(fields()[3].schema(), other.inbank);
        fieldSetFlags()[3] = other.fieldSetFlags()[3];
      }
      if (isValidValue(fields()[4], other.inAccount)) {
        this.inAccount = data().deepCopy(fields()[4].schema(), other.inAccount);
        fieldSetFlags()[4] = other.fieldSetFlags()[4];
      }
      if (isValidValue(fields()[5], other.outbankPartition)) {
        this.outbankPartition = data().deepCopy(fields()[5].schema(), other.outbankPartition);
        fieldSetFlags()[5] = other.fieldSetFlags()[5];
      }
      if (isValidValue(fields()[6], other.inbankPartition)) {
        this.inbankPartition = data().deepCopy(fields()[6].schema(), other.inbankPartition);
        fieldSetFlags()[6] = other.fieldSetFlags()[6];
      }
      if (isValidValue(fields()[7], other.amount)) {
        this.amount = data().deepCopy(fields()[7].schema(), other.amount);
        fieldSetFlags()[7] = other.fieldSetFlags()[7];
      }
      if (isValidValue(fields()[8], other.category)) {
        this.category = data().deepCopy(fields()[8].schema(), other.category);
        fieldSetFlags()[8] = other.fieldSetFlags()[8];
      }
    }

    /**
     * Creates a Builder by copying an existing Transaction instance
     * @param other The existing instance to copy.
     */
    private Builder(my.avroSchema.Transaction other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.serialNumber)) {
        this.serialNumber = data().deepCopy(fields()[0].schema(), other.serialNumber);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.outbank)) {
        this.outbank = data().deepCopy(fields()[1].schema(), other.outbank);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.outAccount)) {
        this.outAccount = data().deepCopy(fields()[2].schema(), other.outAccount);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.inbank)) {
        this.inbank = data().deepCopy(fields()[3].schema(), other.inbank);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.inAccount)) {
        this.inAccount = data().deepCopy(fields()[4].schema(), other.inAccount);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.outbankPartition)) {
        this.outbankPartition = data().deepCopy(fields()[5].schema(), other.outbankPartition);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.inbankPartition)) {
        this.inbankPartition = data().deepCopy(fields()[6].schema(), other.inbankPartition);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.amount)) {
        this.amount = data().deepCopy(fields()[7].schema(), other.amount);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.category)) {
        this.category = data().deepCopy(fields()[8].schema(), other.category);
        fieldSetFlags()[8] = true;
      }
    }

    /**
      * Gets the value of the 'serialNumber' field.
      * @return The value.
      */
    public long getSerialNumber() {
      return serialNumber;
    }


    /**
      * Sets the value of the 'serialNumber' field.
      * @param value The value of 'serialNumber'.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder setSerialNumber(long value) {
      validate(fields()[0], value);
      this.serialNumber = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'serialNumber' field has been set.
      * @return True if the 'serialNumber' field has been set, false otherwise.
      */
    public boolean hasSerialNumber() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'serialNumber' field.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder clearSerialNumber() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'outbank' field.
      * @return The value.
      */
    public java.lang.String getOutbank() {
      return outbank;
    }


    /**
      * Sets the value of the 'outbank' field.
      * @param value The value of 'outbank'.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder setOutbank(java.lang.String value) {
      validate(fields()[1], value);
      this.outbank = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'outbank' field has been set.
      * @return True if the 'outbank' field has been set, false otherwise.
      */
    public boolean hasOutbank() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'outbank' field.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder clearOutbank() {
      outbank = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'outAccount' field.
      * @return The value.
      */
    public java.lang.String getOutAccount() {
      return outAccount;
    }


    /**
      * Sets the value of the 'outAccount' field.
      * @param value The value of 'outAccount'.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder setOutAccount(java.lang.String value) {
      validate(fields()[2], value);
      this.outAccount = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'outAccount' field has been set.
      * @return True if the 'outAccount' field has been set, false otherwise.
      */
    public boolean hasOutAccount() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'outAccount' field.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder clearOutAccount() {
      outAccount = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'inbank' field.
      * @return The value.
      */
    public java.lang.String getInbank() {
      return inbank;
    }


    /**
      * Sets the value of the 'inbank' field.
      * @param value The value of 'inbank'.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder setInbank(java.lang.String value) {
      validate(fields()[3], value);
      this.inbank = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'inbank' field has been set.
      * @return True if the 'inbank' field has been set, false otherwise.
      */
    public boolean hasInbank() {
      return fieldSetFlags()[3];
    }


    /**
      * Clears the value of the 'inbank' field.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder clearInbank() {
      inbank = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /**
      * Gets the value of the 'inAccount' field.
      * @return The value.
      */
    public java.lang.String getInAccount() {
      return inAccount;
    }


    /**
      * Sets the value of the 'inAccount' field.
      * @param value The value of 'inAccount'.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder setInAccount(java.lang.String value) {
      validate(fields()[4], value);
      this.inAccount = value;
      fieldSetFlags()[4] = true;
      return this;
    }

    /**
      * Checks whether the 'inAccount' field has been set.
      * @return True if the 'inAccount' field has been set, false otherwise.
      */
    public boolean hasInAccount() {
      return fieldSetFlags()[4];
    }


    /**
      * Clears the value of the 'inAccount' field.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder clearInAccount() {
      inAccount = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /**
      * Gets the value of the 'outbankPartition' field.
      * @return The value.
      */
    public int getOutbankPartition() {
      return outbankPartition;
    }


    /**
      * Sets the value of the 'outbankPartition' field.
      * @param value The value of 'outbankPartition'.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder setOutbankPartition(int value) {
      validate(fields()[5], value);
      this.outbankPartition = value;
      fieldSetFlags()[5] = true;
      return this;
    }

    /**
      * Checks whether the 'outbankPartition' field has been set.
      * @return True if the 'outbankPartition' field has been set, false otherwise.
      */
    public boolean hasOutbankPartition() {
      return fieldSetFlags()[5];
    }


    /**
      * Clears the value of the 'outbankPartition' field.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder clearOutbankPartition() {
      fieldSetFlags()[5] = false;
      return this;
    }

    /**
      * Gets the value of the 'inbankPartition' field.
      * @return The value.
      */
    public int getInbankPartition() {
      return inbankPartition;
    }


    /**
      * Sets the value of the 'inbankPartition' field.
      * @param value The value of 'inbankPartition'.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder setInbankPartition(int value) {
      validate(fields()[6], value);
      this.inbankPartition = value;
      fieldSetFlags()[6] = true;
      return this;
    }

    /**
      * Checks whether the 'inbankPartition' field has been set.
      * @return True if the 'inbankPartition' field has been set, false otherwise.
      */
    public boolean hasInbankPartition() {
      return fieldSetFlags()[6];
    }


    /**
      * Clears the value of the 'inbankPartition' field.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder clearInbankPartition() {
      fieldSetFlags()[6] = false;
      return this;
    }

    /**
      * Gets the value of the 'amount' field.
      * @return The value.
      */
    public long getAmount() {
      return amount;
    }


    /**
      * Sets the value of the 'amount' field.
      * @param value The value of 'amount'.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder setAmount(long value) {
      validate(fields()[7], value);
      this.amount = value;
      fieldSetFlags()[7] = true;
      return this;
    }

    /**
      * Checks whether the 'amount' field has been set.
      * @return True if the 'amount' field has been set, false otherwise.
      */
    public boolean hasAmount() {
      return fieldSetFlags()[7];
    }


    /**
      * Clears the value of the 'amount' field.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder clearAmount() {
      fieldSetFlags()[7] = false;
      return this;
    }

    /**
      * Gets the value of the 'category' field.
      * @return The value.
      */
    public int getCategory() {
      return category;
    }


    /**
      * Sets the value of the 'category' field.
      * @param value The value of 'category'.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder setCategory(int value) {
      validate(fields()[8], value);
      this.category = value;
      fieldSetFlags()[8] = true;
      return this;
    }

    /**
      * Checks whether the 'category' field has been set.
      * @return True if the 'category' field has been set, false otherwise.
      */
    public boolean hasCategory() {
      return fieldSetFlags()[8];
    }


    /**
      * Clears the value of the 'category' field.
      * @return This builder.
      */
    public my.avroSchema.Transaction.Builder clearCategory() {
      fieldSetFlags()[8] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Transaction build() {
      try {
        Transaction record = new Transaction();
        record.serialNumber = fieldSetFlags()[0] ? this.serialNumber : (java.lang.Long) defaultValue(fields()[0]);
        record.outbank = fieldSetFlags()[1] ? this.outbank : (java.lang.String) defaultValue(fields()[1]);
        record.outAccount = fieldSetFlags()[2] ? this.outAccount : (java.lang.String) defaultValue(fields()[2]);
        record.inbank = fieldSetFlags()[3] ? this.inbank : (java.lang.String) defaultValue(fields()[3]);
        record.inAccount = fieldSetFlags()[4] ? this.inAccount : (java.lang.String) defaultValue(fields()[4]);
        record.outbankPartition = fieldSetFlags()[5] ? this.outbankPartition : (java.lang.Integer) defaultValue(fields()[5]);
        record.inbankPartition = fieldSetFlags()[6] ? this.inbankPartition : (java.lang.Integer) defaultValue(fields()[6]);
        record.amount = fieldSetFlags()[7] ? this.amount : (java.lang.Long) defaultValue(fields()[7]);
        record.category = fieldSetFlags()[8] ? this.category : (java.lang.Integer) defaultValue(fields()[8]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<Transaction>
    WRITER$ = (org.apache.avro.io.DatumWriter<Transaction>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<Transaction>
    READER$ = (org.apache.avro.io.DatumReader<Transaction>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

  @Override protected boolean hasCustomCoders() { return true; }

  @Override public void customEncode(org.apache.avro.io.Encoder out)
    throws java.io.IOException
  {
    out.writeLong(this.serialNumber);

    out.writeString(this.outbank);

    out.writeString(this.outAccount);

    out.writeString(this.inbank);

    out.writeString(this.inAccount);

    out.writeInt(this.outbankPartition);

    out.writeInt(this.inbankPartition);

    out.writeLong(this.amount);

    out.writeInt(this.category);

  }

  @Override public void customDecode(org.apache.avro.io.ResolvingDecoder in)
    throws java.io.IOException
  {
    org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
    if (fieldOrder == null) {
      this.serialNumber = in.readLong();

      this.outbank = in.readString();

      this.outAccount = in.readString();

      this.inbank = in.readString();

      this.inAccount = in.readString();

      this.outbankPartition = in.readInt();

      this.inbankPartition = in.readInt();

      this.amount = in.readLong();

      this.category = in.readInt();

    } else {
      for (int i = 0; i < 9; i++) {
        switch (fieldOrder[i].pos()) {
        case 0:
          this.serialNumber = in.readLong();
          break;

        case 1:
          this.outbank = in.readString();
          break;

        case 2:
          this.outAccount = in.readString();
          break;

        case 3:
          this.inbank = in.readString();
          break;

        case 4:
          this.inAccount = in.readString();
          break;

        case 5:
          this.outbankPartition = in.readInt();
          break;

        case 6:
          this.inbankPartition = in.readInt();
          break;

        case 7:
          this.amount = in.readLong();
          break;

        case 8:
          this.category = in.readInt();
          break;

        default:
          throw new java.io.IOException("Corrupt ResolvingDecoder.");
        }
      }
    }
  }
}









