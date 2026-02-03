package top.rymc.phira.main.network;

import top.rymc.phira.protocol.codec.Encodeable;

public interface ProtocolConvertible<T extends Encodeable> {

    T toProtocol();

}
