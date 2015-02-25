/**
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.clever.Common.smackx.bytestreams.ibb.provider;

import java.io.IOException;
import org.clever.Common.smack.packet.IQ;
import org.clever.Common.smack.packet.PacketExtension;
import org.clever.Common.smack.provider.IQProvider;
import org.clever.Common.smack.provider.PacketExtensionProvider;
import org.clever.Common.smackx.bytestreams.ibb.packet.Data;
import org.clever.Common.smackx.bytestreams.ibb.packet.DataPacketExtension;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Parses an In-Band Bytestream data packet which can be a packet extension of
 * either an IQ stanza or a message stanza.
 * 
 * @author Henning Staib
 */
public class DataPacketProvider implements PacketExtensionProvider, IQProvider {

    @Override
    public PacketExtension parseExtension(XmlPullParser parser) throws XmlPullParserException, IOException  {
        String sessionID = parser.getAttributeValue("", "sid");
        long seq = Long.parseLong(parser.getAttributeValue("", "seq"));
        String data = parser.nextText();
        return new DataPacketExtension(sessionID, seq, data);
    }

    @Override
    public IQ parseIQ(XmlPullParser parser) throws XmlPullParserException, IOException  {
        DataPacketExtension data = (DataPacketExtension) parseExtension(parser);
        IQ iq = new Data(data);
        return iq;
    }

}
