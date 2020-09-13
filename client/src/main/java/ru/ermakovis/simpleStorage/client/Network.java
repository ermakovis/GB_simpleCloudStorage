package ru.ermakovis.simpleStorage.client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

public class Network {
    private Socket socket;
    private ObjectEncoderOutputStream output;
    private ObjectDecoderInputStream input;

    public Network(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);
            output = new ObjectEncoderOutputStream(socket.getOutputStream());
            input = new ObjectDecoderInputStream(socket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Object msg) throws IOException {
        output.writeObject(msg);
    }

    public Object receiveMessage() throws IOException, ClassNotFoundException {
        return input.readObject();
    }

    public void stop() {
        try {
            output.close();
            input.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
