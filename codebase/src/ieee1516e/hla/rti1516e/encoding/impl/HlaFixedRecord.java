/*
 *   Copyright 2020 Open LVC Project.
 *
 *   This file is part of Open LVC HLA IfSpec.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package hla.rti1516e.encoding.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import hla.rti1516e.encoding.ByteWrapper;
import hla.rti1516e.encoding.DataElement;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.encoding.HLAfixedRecord;

public class HlaFixedRecord implements HLAfixedRecord
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private List<DataElement> items;
	private int boundary;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public HlaFixedRecord()
	{
		this.items = new ArrayList<>();
		this.boundary = -1;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	////////////////////////////////////////////////////////////////////////////////////////////
	/// Accessor and Mutator Methods   /////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void add( DataElement item )
	{
		items.add( item );
		resetBoundary();
	}
	
	@Override
	public int size()
	{
		return items.size();
	}
	
	public DataElement get( int index )
	{
		return items.get( index );
	}
	
	@Override
	public Iterator<DataElement> iterator()
	{
		resetBoundary();
		return items.iterator();
	}
	
	@Override
	public int getOctetBoundary()
	{
		return calculateBoundary();
	}

	@Override
	public int getEncodedLength()
	{
		int count = 0;
		for( DataElement dataElement : this.items )
		{
			int boundary = dataElement.getOctetBoundary();
			while( count % boundary != 0 )
				count++;
			count+= dataElement.getEncodedLength();
		}
		
		return count;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/// Encoding Methods   /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void encode( ByteWrapper wrapper ) throws EncoderException
	{
		try
		{
			wrapper.align( getOctetBoundary() );
			for( DataElement item : this.items )
				item.encode( wrapper );
		}
		catch( Exception e )
		{
			throw new EncoderException( "Failed in HLAfixedRecord::encode >> "+e.getMessage(), e );
		}
	}
	
	@Override
	public byte[] toByteArray() throws EncoderException
	{
		ByteWrapper byteWrapper = new ByteWrapper( getEncodedLength() );
		encode( byteWrapper );
		return byteWrapper.array();
	}

	@Override
	public void decode( ByteWrapper wrapper ) throws DecoderException
	{
		try
		{
    		wrapper.align( getOctetBoundary() );
    		for( DataElement item : this.items )
    			item.decode( wrapper );
		}
		catch( Exception e )
		{
			throw new DecoderException( "Failed in HLAfixedRecord::decode >> "+e.getMessage(), e );
		}
	}
	
	@Override
	public void decode( byte[] bytes ) throws DecoderException
	{
		decode( new ByteWrapper(bytes) );
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/// Internal Helper Methods   //////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private final void resetBoundary()
	{
		this.boundary = -1;
	}
	
	private final int calculateBoundary()
	{
		if( this.boundary == -1 )
		{
			// Get the max size of all records. Smallest size of record is one empty byte.
			int temp = 1;
			for( DataElement item : this.items )
				temp = Math.max( temp, item.getOctetBoundary() );
			
			this.boundary = temp;
		}
		
		return this.boundary;
	}
	
	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
