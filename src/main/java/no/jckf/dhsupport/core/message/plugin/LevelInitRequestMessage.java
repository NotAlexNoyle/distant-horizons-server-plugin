package no.jckf.dhsupport.core.message.plugin;

import no.jckf.dhsupport.core.bytestream.Decoder;

public class LevelInitRequestMessage extends PluginMessage
{
    protected String worldKey;

    public String getWorldKey()
    {
        return worldKey;
    }

    @Override
    public void decode(Decoder decoder)
    {
        this.worldKey = decoder.readShortString();
    }
}
