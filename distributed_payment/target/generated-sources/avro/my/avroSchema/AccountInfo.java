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
public class AccountInfo extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 4660485530181356090L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"AccountInfo\",\"namespace\":\"my.avroSchema\",\"fields\":[{\"name\":\"bank\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"bankPartition\",\"type\":\"int\"},{\"name\":\"Accounts\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"Account\",\"fields\":[{\"name\":\"AccountNo\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}}}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();

  private static final BinaryMessageEncoder<AccountInfo> ENCODER =
      new BinaryMessageEncoder<>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<AccountInfo> DECODER =
      new BinaryMessageDecoder<>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<AccountInfo> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<AccountInfo> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<AccountInfo> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this AccountInfo to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a AccountInfo from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a AccountInfo instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static AccountInfo fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  private java.lang.String bank;
  private int bankPartition;
  private java.util.List<my.avroSchema.Account> Accounts;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public AccountInfo() {}

  /**
   * All-args constructor.
   * @param bank The new value for bank
   * @param bankPartition The new value for bankPartition
   * @param Accounts The new value for Accounts
   */
  public AccountInfo(java.lang.String bank, java.lang.Integer bankPartition, java.util.List<my.avroSchema.Account> Accounts) {
    this.bank = bank;
    this.bankPartition = bankPartition;
    this.Accounts = Accounts;
  }

  @Override
  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }

  @Override
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }

  // Used by DatumWriter.  Applications should not call.
  @Override
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return bank;
    case 1: return bankPartition;
    case 2: return Accounts;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  // Used by DatumReader.  Applications should not call.
  @Override
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: bank = value$ != null ? value$.toString() : null; break;
    case 1: bankPartition = (java.lang.Integer)value$; break;
    case 2: Accounts = (java.util.List<my.avroSchema.Account>)value$; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'bank' field.
   * @return The value of the 'bank' field.
   */
  public java.lang.String getBank() {
    return bank;
  }



  /**
   * Gets the value of the 'bankPartition' field.
   * @return The value of the 'bankPartition' field.
   */
  public int getBankPartition() {
    return bankPartition;
  }



  /**
   * Gets the value of the 'Accounts' field.
   * @return The value of the 'Accounts' field.
   */
  public java.util.List<my.avroSchema.Account> getAccounts() {
    return Accounts;
  }



  /**
   * Creates a new AccountInfo RecordBuilder.
   * @return A new AccountInfo RecordBuilder
   */
  public static my.avroSchema.AccountInfo.Builder newBuilder() {
    return new my.avroSchema.AccountInfo.Builder();
  }

  /**
   * Creates a new AccountInfo RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new AccountInfo RecordBuilder
   */
  public static my.avroSchema.AccountInfo.Builder newBuilder(my.avroSchema.AccountInfo.Builder other) {
    if (other == null) {
      return new my.avroSchema.AccountInfo.Builder();
    } else {
      return new my.avroSchema.AccountInfo.Builder(other);
    }
  }

  /**
   * Creates a new AccountInfo RecordBuilder by copying an existing AccountInfo instance.
   * @param other The existing instance to copy.
   * @return A new AccountInfo RecordBuilder
   */
  public static my.avroSchema.AccountInfo.Builder newBuilder(my.avroSchema.AccountInfo other) {
    if (other == null) {
      return new my.avroSchema.AccountInfo.Builder();
    } else {
      return new my.avroSchema.AccountInfo.Builder(other);
    }
  }

  /**
   * RecordBuilder for AccountInfo instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<AccountInfo>
    implements org.apache.avro.data.RecordBuilder<AccountInfo> {

    private java.lang.String bank;
    private int bankPartition;
    private java.util.List<my.avroSchema.Account> Accounts;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$, MODEL$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(my.avroSchema.AccountInfo.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.bank)) {
        this.bank = data().deepCopy(fields()[0].schema(), other.bank);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.bankPartition)) {
        this.bankPartition = data().deepCopy(fields()[1].schema(), other.bankPartition);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
      if (isValidValue(fields()[2], other.Accounts)) {
        this.Accounts = data().deepCopy(fields()[2].schema(), other.Accounts);
        fieldSetFlags()[2] = other.fieldSetFlags()[2];
      }
    }

    /**
     * Creates a Builder by copying an existing AccountInfo instance
     * @param other The existing instance to copy.
     */
    private Builder(my.avroSchema.AccountInfo other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.bank)) {
        this.bank = data().deepCopy(fields()[0].schema(), other.bank);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.bankPartition)) {
        this.bankPartition = data().deepCopy(fields()[1].schema(), other.bankPartition);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.Accounts)) {
        this.Accounts = data().deepCopy(fields()[2].schema(), other.Accounts);
        fieldSetFlags()[2] = true;
      }
    }

    /**
      * Gets the value of the 'bank' field.
      * @return The value.
      */
    public java.lang.String getBank() {
      return bank;
    }


    /**
      * Sets the value of the 'bank' field.
      * @param value The value of 'bank'.
      * @return This builder.
      */
    public my.avroSchema.AccountInfo.Builder setBank(java.lang.String value) {
      validate(fields()[0], value);
      this.bank = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'bank' field has been set.
      * @return True if the 'bank' field has been set, false otherwise.
      */
    public boolean hasBank() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'bank' field.
      * @return This builder.
      */
    public my.avroSchema.AccountInfo.Builder clearBank() {
      bank = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'bankPartition' field.
      * @return The value.
      */
    public int getBankPartition() {
      return bankPartition;
    }


    /**
      * Sets the value of the 'bankPartition' field.
      * @param value The value of 'bankPartition'.
      * @return This builder.
      */
    public my.avroSchema.AccountInfo.Builder setBankPartition(int value) {
      validate(fields()[1], value);
      this.bankPartition = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'bankPartition' field has been set.
      * @return True if the 'bankPartition' field has been set, false otherwise.
      */
    public boolean hasBankPartition() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'bankPartition' field.
      * @return This builder.
      */
    public my.avroSchema.AccountInfo.Builder clearBankPartition() {
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'Accounts' field.
      * @return The value.
      */
    public java.util.List<my.avroSchema.Account> getAccounts() {
      return Accounts;
    }


    /**
      * Sets the value of the 'Accounts' field.
      * @param value The value of 'Accounts'.
      * @return This builder.
      */
    public my.avroSchema.AccountInfo.Builder setAccounts(java.util.List<my.avroSchema.Account> value) {
      validate(fields()[2], value);
      this.Accounts = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'Accounts' field has been set.
      * @return True if the 'Accounts' field has been set, false otherwise.
      */
    public boolean hasAccounts() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'Accounts' field.
      * @return This builder.
      */
    public my.avroSchema.AccountInfo.Builder clearAccounts() {
      Accounts = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AccountInfo build() {
      try {
        AccountInfo record = new AccountInfo();
        record.bank = fieldSetFlags()[0] ? this.bank : (java.lang.String) defaultValue(fields()[0]);
        record.bankPartition = fieldSetFlags()[1] ? this.bankPartition : (java.lang.Integer) defaultValue(fields()[1]);
        record.Accounts = fieldSetFlags()[2] ? this.Accounts : (java.util.List<my.avroSchema.Account>) defaultValue(fields()[2]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<AccountInfo>
    WRITER$ = (org.apache.avro.io.DatumWriter<AccountInfo>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<AccountInfo>
    READER$ = (org.apache.avro.io.DatumReader<AccountInfo>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

  @Override protected boolean hasCustomCoders() { return true; }

  @Override public void customEncode(org.apache.avro.io.Encoder out)
    throws java.io.IOException
  {
    out.writeString(this.bank);

    out.writeInt(this.bankPartition);

    long size0 = this.Accounts.size();
    out.writeArrayStart();
    out.setItemCount(size0);
    long actualSize0 = 0;
    for (my.avroSchema.Account e0: this.Accounts) {
      actualSize0++;
      out.startItem();
      e0.customEncode(out);
    }
    out.writeArrayEnd();
    if (actualSize0 != size0)
      throw new java.util.ConcurrentModificationException("Array-size written was " + size0 + ", but element count was " + actualSize0 + ".");

  }

  @Override public void customDecode(org.apache.avro.io.ResolvingDecoder in)
    throws java.io.IOException
  {
    org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
    if (fieldOrder == null) {
      this.bank = in.readString();

      this.bankPartition = in.readInt();

      long size0 = in.readArrayStart();
      java.util.List<my.avroSchema.Account> a0 = this.Accounts;
      if (a0 == null) {
        a0 = new SpecificData.Array<my.avroSchema.Account>((int)size0, SCHEMA$.getField("Accounts").schema());
        this.Accounts = a0;
      } else a0.clear();
      SpecificData.Array<my.avroSchema.Account> ga0 = (a0 instanceof SpecificData.Array ? (SpecificData.Array<my.avroSchema.Account>)a0 : null);
      for ( ; 0 < size0; size0 = in.arrayNext()) {
        for ( ; size0 != 0; size0--) {
          my.avroSchema.Account e0 = (ga0 != null ? ga0.peek() : null);
          if (e0 == null) {
            e0 = new my.avroSchema.Account();
          }
          e0.customDecode(in);
          a0.add(e0);
        }
      }

    } else {
      for (int i = 0; i < 3; i++) {
        switch (fieldOrder[i].pos()) {
        case 0:
          this.bank = in.readString();
          break;

        case 1:
          this.bankPartition = in.readInt();
          break;

        case 2:
          long size0 = in.readArrayStart();
          java.util.List<my.avroSchema.Account> a0 = this.Accounts;
          if (a0 == null) {
            a0 = new SpecificData.Array<my.avroSchema.Account>((int)size0, SCHEMA$.getField("Accounts").schema());
            this.Accounts = a0;
          } else a0.clear();
          SpecificData.Array<my.avroSchema.Account> ga0 = (a0 instanceof SpecificData.Array ? (SpecificData.Array<my.avroSchema.Account>)a0 : null);
          for ( ; 0 < size0; size0 = in.arrayNext()) {
            for ( ; size0 != 0; size0--) {
              my.avroSchema.Account e0 = (ga0 != null ? ga0.peek() : null);
              if (e0 == null) {
                e0 = new my.avroSchema.Account();
              }
              e0.customDecode(in);
              a0.add(e0);
            }
          }
          break;

        default:
          throw new java.io.IOException("Corrupt ResolvingDecoder.");
        }
      }
    }
  }
}










