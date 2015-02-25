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
package org.clever.Common.smackx.pubsub.provider;

import org.clever.Common.smack.packet.IQ;
import org.clever.Common.smack.packet.PacketExtension;
import org.clever.Common.smack.provider.IQProvider;
import org.clever.Common.smack.util.PacketParserUtils;
import org.clever.Common.smackx.pubsub.packet.PubSub;
import org.clever.Common.smackx.pubsub.packet.PubSubNamespace;
import org.xmlpull.v1.XmlPullParser;

/**
 * Parses the root pubsub packet extensions of the {@link IQ} packet and returns
 * a {@link PubSub} instance.
 * 
 * @author Robin Collier
 */
public class PubSubProvider implements IQProvider
{
	public IQ parseIQ(XmlPullParser parser) throws Exception
	{
        PubSub pubsub = new PubSub();
        String namespace = parser.getNamespace();
        pubsub.setPubSubNamespace(PubSubNamespace.valueOfFromXmlns(namespace));
        boolean done = false;

        while (!done) 
        {
            int eventType = parser.next();
            
            if (eventType == XmlPullParser.START_TAG) 
            {
            	PacketExtension ext = PacketParserUtils.parsePacketExtension(parser.getName(), namespace, parser);
            	
            	if (ext != null)
            	{
            		pubsub.addExtension(ext);
            	}
            }
            else if (eventType == XmlPullParser.END_TAG) 
            {
                if (parser.getName().equals("pubsub")) 
                {
                    done = true;
                }
            }
        }
        return pubsub;
	}
}
