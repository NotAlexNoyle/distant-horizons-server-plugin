/*
 * DH Support, server-side support for Distant Horizons.
 * Copyright (C) 2024 Jim C K Flaten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import junit.framework.TestCase;
import no.jckf.dhsupport.core.Utils;
import no.jckf.dhsupport.core.bytestream.Decoder;
import no.jckf.dhsupport.core.bytestream.Encoder;
import no.jckf.dhsupport.core.dataobject.DataPoint;

public class DataPointTest extends TestCase
{
    private DataPoint dataPoint = new DataPoint();

    public void testMappingId()
    {
        int randomNumber = (int) Math.floor(Math.random() * 100);

        this.dataPoint.setMappingId(randomNumber);

        assertEquals(randomNumber, this.dataPoint.getMappingId());
    }

    public void testMappingIdAgain()
    {
        this.testMappingId();
    }

    public void testHeight()
    {
        int randomNumber = (int) Math.floor(Math.random() * 10);

        this.dataPoint.setHeight(randomNumber);

        assertEquals(randomNumber, this.dataPoint.getHeight());
    }

    public void testHeightAgain()
    {
        this.testHeight();
    }

    public void testStartY()
    {
        int randomNumber = (int) Math.floor(Math.random() * 100);

        this.dataPoint.setStartY(randomNumber);

        assertEquals(randomNumber, this.dataPoint.getStartY());
    }

    public void testHighStartY()
    {
        int randomNumber = 4000; // Chosen by fair dice roll. Guaranteed to be random.

        this.dataPoint.setStartY(randomNumber);

        assertEquals(randomNumber, this.dataPoint.getStartY());
    }

    public void testStartYAgain()
    {
        this.testStartY();
    }

    public void testSkyLight()
    {
        byte randomNumber = (byte) Math.floor(Math.random() * 15);

        this.dataPoint.setSkyLight(randomNumber);

        assertEquals(randomNumber, this.dataPoint.getSkyLight());
    }

    public void testSkyLightAgain()
    {
        this.testSkyLight();
    }

    public void testBlockLight()
    {
        byte randomNumber = (byte) Math.floor(Math.random() * 15);

        this.dataPoint.setBlockLight(randomNumber);

        assertEquals(randomNumber, this.dataPoint.getBlockLight());
    }

    public void testBlockLightAgain()
    {
        this.testBlockLight();
    }

    public void testEncodeDecode()
    {
        int mappingId = (int) Math.floor(Math.random() * 100);
        int height = (int) Math.floor(Math.random() * 10);
        int startY = (int) Math.floor(Math.random() * 100);
        byte skyLight = (byte) Math.floor(Math.random() * 15);
        byte blockLight = (byte) Math.floor(Math.random() * 15);

        // Set them in reverse order of how they appear in the packed long. This will reveal any accidental overwrites.
        this.dataPoint.setBlockLight(blockLight);
        this.dataPoint.setSkyLight(skyLight);
        this.dataPoint.setStartY(startY);
        this.dataPoint.setHeight(height);
        this.dataPoint.setMappingId(mappingId);

        Encoder encoder = new Encoder();

        this.dataPoint.encode(encoder);

        this.dataPoint = new DataPoint();

        Decoder decoder = new Decoder(encoder.toByteArray());

        this.dataPoint.decode(decoder);

        assertEquals("Block light", blockLight, this.dataPoint.getBlockLight());
        assertEquals("Sky light", skyLight, this.dataPoint.getSkyLight());
        assertEquals("Start Y", startY, this.dataPoint.getStartY());
        assertEquals("Height", height, this.dataPoint.getHeight());
        assertEquals("Mapping ID", mappingId, this.dataPoint.getMappingId());
    }

    public void testBinary()
    {
        this.dataPoint = new DataPoint();

        this.dataPoint.setMappingId(1);
        Encoder mappingIdEncoder = new Encoder();
        this.dataPoint.encode(mappingIdEncoder);
        this.dataPoint.setMappingId(0);

        this.dataPoint.setHeight(1);
        Encoder heightEncoder = new Encoder();
        this.dataPoint.encode(heightEncoder);
        this.dataPoint.setHeight(0);

        this.dataPoint.setStartY(1);
        Encoder startYEncoder = new Encoder();
        this.dataPoint.encode(startYEncoder);
        this.dataPoint.setStartY(0);

        this.dataPoint.setSkyLight((byte) 1);
        Encoder skyLightEncoder = new Encoder();
        this.dataPoint.encode(skyLightEncoder);
        this.dataPoint.setSkyLight((byte) 0);

        this.dataPoint.setBlockLight((byte) 1);
        Encoder blockLightEncoder = new Encoder();
        this.dataPoint.encode(blockLightEncoder);
        this.dataPoint.setBlockLight((byte) 0);

        //                          | Blk| Sky|       Start Y|        Height|                             Mapping ID|
        assertEquals("Mapping ID",  "0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0001", Utils.bytesToBin(mappingIdEncoder.toByteArray()));
        assertEquals("Height",      "0000 0000 0000 0000 0000 0000 0000 0001 0000 0000 0000 0000 0000 0000 0000 0000", Utils.bytesToBin(heightEncoder.toByteArray()));
        assertEquals("StartY",      "0000 0000 0000 0000 0001 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000", Utils.bytesToBin(startYEncoder.toByteArray()));
        assertEquals("Sky light",   "0000 0001 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000", Utils.bytesToBin(skyLightEncoder.toByteArray()));
        assertEquals("Block light", "0001 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000", Utils.bytesToBin(blockLightEncoder.toByteArray()));
    }
}
