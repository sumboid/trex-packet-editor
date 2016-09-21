package com.xored.javafx.packeteditor.data;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.xored.javafx.packeteditor.scapy.ScapyPkt;
import com.xored.javafx.packeteditor.scapy.ScapyServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;

import static com.xored.javafx.packeteditor.scapy.ScapyUtils.createReconstructPktPayload;

public class PacketDataController extends Observable {
    static Logger log = LoggerFactory.getLogger(PacketDataController.class);
    @Inject ScapyServerClient scapy;
    @Inject IBinaryData binary;

    ScapyPkt pkt;

    public void init() {
        scapy.open("tcp://localhost:4507");
        ClassLoader classLoader = getClass().getClassLoader();
        File example_file = new File(classLoader.getResource("http_get_request.pcap").getFile());
        try {
            loadPcapFile(example_file);
        } catch (Exception e) {
            log.error("{}", e);
        }
    }

    public void replacePacket(ScapyPkt payload) {
        pkt = payload;
        byte [] bytes = pkt.getBinaryData();
        setChanged();
        binary.setBytes(bytes);
        notifyObservers(null);
    }

    public JsonArray getProtocols() {
        return pkt.getProtocols();
    }

    public void loadPcapFile(String filename) throws Exception {
        loadPcapFile(new File(filename));
    }

    public void loadPcapFile(File file) throws Exception {
        byte[] bytes = Files.toByteArray(file);
        replacePacket(scapy.read_pcap_packet(bytes));
    }

    public void writeToPcapFile(File file) throws Exception {
        byte[] pcap_bin = scapy.write_pcap_packet(pkt.getBinaryData());
        Files.write(pcap_bin, file);
    }

    public void modifyPacketField(IField field, String newValue) {
        if (field.getPath() == null) {
            log.warn("Can't modify field {} to {}", field, newValue);
            return;
        }
        modifyPacketField(field.getPath(), field.getId(), newValue);
    }

    public void modifyPacketField(List<String> fieldPath, String fieldName, String newValue) {
        try {
            ScapyPkt newPkt = new ScapyPkt(scapy.reconstruct_pkt(pkt.getBinaryData(), createReconstructPktPayload(fieldPath, fieldName, newValue)));
            replacePacket(newPkt);
        } catch (Exception e) {
            log.error("Can't modify: {}", e);

        }
    }
}
