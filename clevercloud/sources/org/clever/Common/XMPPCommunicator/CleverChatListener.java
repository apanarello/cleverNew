/*
 *  Copyright (c) 2010 Antonio Nastasi
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */
package org.clever.Common.XMPPCommunicator;


import org.apache.log4j.Logger;

import org.clever.Common.smack.Chat;
import org.clever.Common.smack.MessageListener;
import org.clever.Common.smack.packet.Message;



public class CleverChatListener implements MessageListener
{

  private CleverMessageHandler msgHandler = null;
  private Logger logger = null;



  CleverChatListener( CleverMessageHandler msgHandler )
  {
    logger = Logger.getLogger( "XMPPCommunicator" );
    this.msgHandler = msgHandler;
  }



  @Override
  public void processMessage( Chat chat, Message msg )
  {
    CleverMessage message = new CleverMessage( msg.getBody() );
    msgHandler.handleCleverMessage( message );


  }
}
