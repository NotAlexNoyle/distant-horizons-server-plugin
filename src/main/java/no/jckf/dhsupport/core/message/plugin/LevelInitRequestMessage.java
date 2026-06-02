package no.jckf.dhsupport.core.message.plugin;

import no.jckf.dhsupport.core.bytestream.Decoder;

public class LevelInitRequestMessage extends PluginMessage
{
    protected String foo;

    @Override
    public void decode(Decoder decoder)
    {
        this.foo = decoder.readShortString();
        System.out.println(this.foo);
    }
}
