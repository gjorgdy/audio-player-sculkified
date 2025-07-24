package de.maxhenkel.audioplayer.nodes;

import de.maxhenkel.audioplayer.MultiLocationalAudioChannel;
import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.ServerPosition;

import java.util.UUID;

public class SourceNode extends AudioNode {

    public final MultiLocationalAudioChannel channel;
    private UUID id;

    @Override
    public void disconnect() {
        super.disconnect();
        PlayerManager.instance().stop(id);
    }

    public SourceNode(ServerPosition position) {
        super(position, false, true);
        this.id = UUID.randomUUID();
        this.channel = new MultiLocationalAudioChannel( "speaker", 16f, this.id);
    }

    public void setId(UUID id) {
        this.id = id;
    }

}
