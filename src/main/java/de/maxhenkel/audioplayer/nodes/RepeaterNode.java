package de.maxhenkel.audioplayer.nodes;

import de.maxhenkel.audioplayer.ServerPosition;

public class RepeaterNode extends AudioNode {

    public RepeaterNode(ServerPosition serverPosition) {
        super(serverPosition, true, true);
    }

}
