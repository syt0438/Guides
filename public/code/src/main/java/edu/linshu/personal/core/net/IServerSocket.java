package edu.linshu.personal.core.net;

import edu.linshu.personal.core.net.nio.ISelector;

import java.io.IOException;
import java.net.SocketAddress;

public interface IServerSocket extends ISocket, ISelector {
    void bind(String host, Integer port) throws IOException;

    IClientSocket accept() throws IOException;

    SocketAddress getLocalSocketAddress() throws IOException;
}
