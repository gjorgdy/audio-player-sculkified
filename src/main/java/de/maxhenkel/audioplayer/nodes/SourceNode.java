package de.maxhenkel.audioplayer.nodes;

import de.maxhenkel.audioplayer.PlayerManager;
import de.maxhenkel.audioplayer.ServerPosition;

import java.util.UUID;

public class SourceNode extends AudioNode {

    private UUID playerID = null;

    @Override
    public void disconnect() {
        super.disconnect();
        PlayerManager.instance().stop(playerID);
    }

    public SourceNode(ServerPosition position) {
        super(position, false, true);
    }

    public void setPlayerID(UUID playerID) {
        this.playerID = playerID;
    }

}
