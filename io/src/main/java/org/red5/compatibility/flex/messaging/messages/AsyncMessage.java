/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.compatibility.flex.messaging.messages;

import org.red5.io.amf3.ByteArray;
import org.red5.io.amf3.IDataInput;
import org.red5.io.amf3.IDataOutput;
import org.red5.io.amf3.IExternalizable;
import org.red5.io.utils.RandomGUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;

/**
 * Base class for for asynchronous Flex compatibility messages.
 *
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */



public class AsyncMessage extends AbstractMessage implements IExternalizable, Serializable {

    private static final long serialVersionUID = -3549535089417916783L;

    protected static byte CORRELATION_ID_FLAG = 1;

    protected static byte CORRELATION_ID_BYTES_FLAG = 2;

    /** Id of message this message belongs to. */
    private String correlationId;

    private byte[] correlationIdBytes;

    private transient AsyncMessage proxiedMessage;

    static Logger log = LoggerFactory.getLogger(AsyncMessage.class);

    public AsyncMessage() {
    }

    public AsyncMessage(AsyncMessage proxiedMessage) {
        this.proxiedMessage = proxiedMessage;
    }

    public void setProxiedMessage(AsyncMessage proxiedMessage) {
        this.proxiedMessage = proxiedMessage;
    }

    public AsyncMessage getProxiedMessage() {
        return proxiedMessage;
    }

    public void setCorrelationId(String id) {
        this.correlationId = id;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    /** {@inheritDoc} */
    @Override
    protected void addParameters(StringBuilder result) {
        super.addParameters(result);
        result.append(",correlationId=");
        result.append(correlationId);
    }

    @Override
    public void writeExternal(IDataOutput output) {
        try {
            if (proxiedMessage != null) {
                proxiedMessage.writeExternal(output);
                return;
            }

            super.writeExternal(output);

            if (this.correlationIdBytes == null) {
                this.correlationIdBytes = RandomGUID.toByteArray(this.correlationId);
            }
            short flags = 0;
            if (this.correlationId != null && this.correlationIdBytes == null) {
                flags |= CORRELATION_ID_FLAG;
            }
            if (this.correlationIdBytes != null) {
                flags |= CORRELATION_ID_BYTES_FLAG;
            }

            output.writeByte((byte) flags);

            if (this.correlationId != null && this.correlationIdBytes == null) {
                output.writeObject(this.correlationId);
            }
            if (this.correlationIdBytes != null) {
                output.writeObject(this.correlationIdBytes);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'écriture de l'objet AsyncMessage", e);
        }
    }

    @Override
    public void readExternal(IDataInput input) {
        try {
            if (proxiedMessage != null) {
                proxiedMessage.readExternal(input);
                return;
            }
            super.readExternal(input);
            short[] flagsArray = readFlags(input);
            for (int i = 0; i < flagsArray.length; ++i) {
                short flags = flagsArray[i];
                short reservedPosition = 0;

                if (i == 0) {
                    if ((flags & CORRELATION_ID_FLAG) != 0) {
                        correlationId = (String) input.readObject();
                    }
                    if ((flags & CORRELATION_ID_BYTES_FLAG) != 0) {
                        ByteArray ba = (ByteArray) input.readObject();
                        correlationIdBytes = new byte[ba.length()];
                        ba.readBytes(correlationIdBytes);
                        correlationId = RandomGUID.fromByteArray(correlationIdBytes);
                    }
                    reservedPosition = 2;
                }
                if (flags >> reservedPosition == 0) {
                    continue;
                }
                for (short j = reservedPosition; j < 6; j++) {
                    if ((flags >> j & 0x1) == 0) {
                        continue;
                    }
                    input.readObject();
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la lecture de l'objet AsyncMessage", e);
        }
    }
}
