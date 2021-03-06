package test.com.blootoothtester.network.linklayer.bt;

import java.nio.charset.Charset;

import test.com.blootoothtester.util.Constants;

/**
 * NOTE: This class heavily relies on sizes of fields and number of users. Please modify any time
 * any of them change.
 */
@SuppressWarnings("WeakerAccess") // TODO: Remove once finalised
public class LinkLayerPdu {

    // priority wise - MESSAGE > REPEAT
    enum Type {
        MESSAGE,
        REPEAT,
        INIT
    }

    private final byte mSessionId;
    private final byte[] mAckArray;
    private final Type mType;

    // applicable to Type Message and Repeat
    private byte mSequenceId;
    private byte mFromId;
    private byte mToId;
    private byte[] mData;

    // applicable to type Repeat
    private byte mRepeaterId; // to know who's repeating besides who is being repeated


    // TODO: Allow NACK to be requested from specific phones e.g. phones whose responses you
    // receive a lot, who are also currently not transmitting a message of their own or a response
    // to a NACK
    // JUSTIFICATION: Experimentally discovered that most devices which transmit well also receive
    // well as the root cause is good BT hardware.
    // Also, since BT scanning is active, you seeing it <=> it can see you

    private final static Charset CHARSET = Charset.forName("UTF-8");

    private final static int TOT_SIZE = 248;
    private final static int ADDR_SIZE_BYTES = 1;
    private final static int PDU_PREFIX_BYTES = getPduPrefix().length;
    private final static int PDU_SESSION_ID_BYTES = 1;
    private final static int PDU_TYPE_BYTES = 1;
    private final static int PDU_SEQ_ID_BYTES = 1;
    private final static int PDU_ACK_ARRAY_BYTES = Constants.MAX_USERS; // 1 ACK byte per user

    private final static int PDU_MSG_HEADER_BYTES = PDU_PREFIX_BYTES
            + PDU_SESSION_ID_BYTES
            + PDU_TYPE_BYTES
            + PDU_ACK_ARRAY_BYTES
            + PDU_SEQ_ID_BYTES
            + ADDR_SIZE_BYTES * 2;

    private final static int PDU_REPEAT_HEADER_BYTES = PDU_PREFIX_BYTES
            + PDU_SESSION_ID_BYTES
            + PDU_TYPE_BYTES
            + PDU_ACK_ARRAY_BYTES
            + PDU_SEQ_ID_BYTES
            + ADDR_SIZE_BYTES * 3;

    private final static int PDU_INIT_HEADER_BYTES = PDU_PREFIX_BYTES
            + PDU_SESSION_ID_BYTES
            + PDU_TYPE_BYTES
            + PDU_ACK_ARRAY_BYTES
            + ADDR_SIZE_BYTES;

    private final static int PAYLOAD_MAX_BYTES = TOT_SIZE - PDU_REPEAT_HEADER_BYTES;

    private LinkLayerPdu(byte sessionId, byte[] ackArray, byte sequenceId, byte fromId, byte toId,
                         byte[] data, Type type, byte repeaterId) {

        mType = type;

        mFromId = fromId;
        mToId = toId;
        mAckArray = ackArray;
        if (mAckArray.length != PDU_ACK_ARRAY_BYTES) {
            throw new IllegalArgumentException("Expected ack array of length " + PDU_ACK_ARRAY_BYTES
                    + " but received length " + mAckArray.length);
        }
        mSessionId = sessionId;
        mSequenceId = sequenceId;

        mData = data;

        if (mData.length > PAYLOAD_MAX_BYTES) {
            throw new IllegalArgumentException("Payload size greater than max (received "
                    + data.length + " max " + PAYLOAD_MAX_BYTES + " bytes)");
        }

        mRepeaterId = repeaterId;
    }

    public static LinkLayerPdu getAckChangedPdu(LinkLayerPdu oldPdu, byte[] newAckArray) {
        return new LinkLayerPdu(oldPdu.getSessionId(), newAckArray, oldPdu.getSequenceId(),
                oldPdu.getFromAddress(), oldPdu.getToAddress(), oldPdu.getData(),
                oldPdu.getType(), oldPdu.getRepeaterAddress());
    }

    public static LinkLayerPdu getMessagePdu(byte sessionId, byte[] ackArray, byte sequenceId,
                                             byte fromId, byte toId,
                                             byte[] data) {
        return new LinkLayerPdu(sessionId, ackArray, sequenceId, fromId, toId, data,
                Type.MESSAGE, (byte) 0);
    }

    /**
     * @param ackArray   the AckArray of the repeater (own ack array)
     * @param repeaterId the ID of the device repeating the message (own ID)
     * @param repeatPdu  the PDU being re-broadcasted. Kept it this way in case we remove LlMessage
     *                   in the future
     * @return a REPEAT type LL PDU
     */
    public static LinkLayerPdu getRepeatPdu(byte[] ackArray, byte repeaterId,
                                            LinkLayerPdu repeatPdu) {
        return new LinkLayerPdu(
                repeatPdu.getSessionId(),
                ackArray,
                repeatPdu.getSequenceId(),
                repeatPdu.getFromAddress(),
                repeatPdu.getToAddress(),
                repeatPdu.getData(),
                Type.REPEAT,
                repeaterId
        );
    }

    public static LinkLayerPdu getInitPdu(byte sessionId, byte[] ackArray, byte fromId) {
        return new LinkLayerPdu(
                sessionId,
                ackArray,
                (byte) 0,
                fromId,
                (byte) 0,
                null,
                Type.INIT,
                (byte) 0
        );
    }

    public static boolean isValidPdu(String encoded, byte sessionId) {
        return encoded != null && isValidPdu(encoded.getBytes(CHARSET), sessionId);
    }

    public static boolean isValidPdu(byte[] encoded, byte sessionId) {
        byte[] prefix = getPduPrefix();
        if (encoded.length < prefix.length + PDU_SESSION_ID_BYTES + 2 * ADDR_SIZE_BYTES) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (encoded[i] != prefix[i]) {
                return false;
            }
        }
        return encoded[prefix.length] == sessionId;
    }

    public String getAsString() {
        return new String(encode(), CHARSET);
    }


    /**
     * adds 1 to ordinal value of type as encoding since 0 truncates BT string
     *
     * @return type encoded into a single byte
     */
    private static byte getTypeEncoded(Type type) {
        return (byte) (type.ordinal() + 1);
    }

    /**
     * subtracts 1 from ordinal value of type and uses it to obtain the Type object,
     * as 1 is added to the ordinal value in encoding
     *
     * @return Type that represents the given byte
     */
    private static Type getTypeDecoded(byte type) {
        return Type.values()[type - 1];
    }

    private byte[] encode() {
        byte[] prefix = getPduPrefix();
        int headerSize = 0;
        switch (getType()) {
            case MESSAGE:
                headerSize = PDU_MSG_HEADER_BYTES;
                break;
            case REPEAT:
                headerSize = PDU_REPEAT_HEADER_BYTES;
                break;
            case INIT:
                headerSize = PDU_INIT_HEADER_BYTES;
                break;

        }
        byte[] encoded = new byte[headerSize + mData.length];
        // add prefix
        System.arraycopy(prefix, 0, encoded, 0, prefix.length);
        int nextFieldIndex = prefix.length;
        // add session ID
        encoded[nextFieldIndex] = mSessionId;
        nextFieldIndex += PDU_SESSION_ID_BYTES;
        // add Type
        encoded[nextFieldIndex] = getTypeEncoded(mType);
        nextFieldIndex += PDU_TYPE_BYTES;
        // add ACK array
        System.arraycopy(mAckArray, 0, encoded, nextFieldIndex, mAckArray.length);
        nextFieldIndex += PDU_ACK_ARRAY_BYTES;
        // add fromID
        encoded[nextFieldIndex] = mFromId;
        nextFieldIndex += ADDR_SIZE_BYTES;
        // if INIT, this is all we need
        if (getType() == Type.INIT) {
            return encoded;
        }
        // if REPEAT add repeaterId
        if (getType() == Type.REPEAT) {
            encoded[nextFieldIndex] = getRepeaterAddress();
            nextFieldIndex += ADDR_SIZE_BYTES;
        }
        // add toID
        encoded[nextFieldIndex] = mToId;
        nextFieldIndex += ADDR_SIZE_BYTES;
        // add Sequence ID for message
        encoded[nextFieldIndex] = mSequenceId;
        nextFieldIndex += PDU_SEQ_ID_BYTES;
        // add the actual data to send
        System.arraycopy(mData, 0, encoded, nextFieldIndex, mData.length);
        return encoded;
    }

    private static LinkLayerPdu decode(byte[] encoded) {
        byte[] prefix = getPduPrefix();
        int nextFieldIndex = prefix.length;
        // get session ID
        byte sessionId = encoded[nextFieldIndex];
        nextFieldIndex += PDU_SESSION_ID_BYTES;
        // get type
        Type type = getTypeDecoded(encoded[nextFieldIndex]);
        nextFieldIndex += PDU_TYPE_BYTES;
        // get ACK array
        byte[] ackArray = new byte[Constants.MAX_USERS];
        System.arraycopy(encoded, nextFieldIndex, ackArray, 0, ackArray.length);
        nextFieldIndex += PDU_ACK_ARRAY_BYTES;
        // get fromID
        byte fromId = encoded[nextFieldIndex];
        nextFieldIndex += ADDR_SIZE_BYTES;
        // if INIT, we are done
        if (type == Type.INIT) {
            return new LinkLayerPdu(sessionId, ackArray, (byte) 0, fromId, (byte) 0, null, type,
                    (byte) 0);
        }
        // if REPEAT, get repeaterId
        byte repeaterId = 0;
        if (type == Type.REPEAT) {
            repeaterId = encoded[nextFieldIndex];
            nextFieldIndex += ADDR_SIZE_BYTES;
        }
        // get toID
        byte toId = encoded[nextFieldIndex];
        nextFieldIndex += ADDR_SIZE_BYTES;
        // get Sequence ID for message
        byte sequenceId = encoded[nextFieldIndex];
        nextFieldIndex += PDU_SEQ_ID_BYTES;
        // get the actual data
        byte[] data = new byte[encoded.length - nextFieldIndex];
        System.arraycopy(encoded, nextFieldIndex, data, 0, data.length);

        return new LinkLayerPdu(sessionId, ackArray, sequenceId, fromId, toId, data, type,
                repeaterId);
    }

    public static LinkLayerPdu from(String encoded) {
        return decode(encoded.getBytes(CHARSET));
    }

    public static byte[] getPduPrefix() {
        return new byte[]{(byte) 21, (byte) 22, (byte) 23};
    }

    public byte[] getData() {
        return mData;
    }

    /**
     * temporary while only link layer is present
     */
    public String getDataAsString() {
        return new String(mData, CHARSET);
    }

    public byte getSequenceId() {
        return mSequenceId;
    }

    // TODO: use to differentiate sessions
    public byte getSessionId() {
        return mSequenceId;
    }

    public byte getFromAddress() {
        return mFromId;
    }

    public byte getToAddress() {
        return mToId;
    }

    public Type getType() {
        return mType;
    }

    public byte getRepeaterAddress() {
        return mRepeaterId;
    }

    public byte[] getAckArray() {
        return mAckArray;
    }
}
