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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import hla.rti1516e.encoding.ByteWrapper;
import hla.rti1516e.encoding.DataElement;
import hla.rti1516e.encoding.DataElementFactory;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderException;
import hla.rti1516e.encoding.HLAvariableArray;

public class HlaVariableArray<T extends DataElement> implements HLAvariableArray<T>
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private List<T> items;
	private DataElementFactory<T> factory;
	private int boundary;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	@SuppressWarnings("unchecked")
	public HlaVariableArray( DataElementFactory<T> factory, T... initialItems )
	{
		this.items = new ArrayList<>();
		this.factory = factory;
		this.boundary = -1;
		this.items.addAll( Arrays.asList(initialItems) );
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/// Accessor and Mutator Methods   /////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void addElement( T element )
	{
		items.add( element );
		resetBoundary();
	}

	@Override
	public int size()
	{
		return items.size();
	}

	@Override
	public T get( int index )
	{
		return items.get( index );
	}

	@Override
	public Iterator<T> iterator()
	{
		resetBoundary();
		return items.iterator();
	}

	@Override
	public int getEncodedLength()
	{
		int length = 4;
		for( DataElement item : this.items )
		{
			// add padding
			while( length % item.getOctetBoundary() != 0 )
				length++;
			
			// add item length
			length += item.getEncodedLength();
		}

		return length;
	}

	@Override
	public int getOctetBoundary()
	{
		return calculateBoundary();
	}

	@Override
	public void resize( int size )
	{
		if( size < items.size() )
		{
			// we have too many elements; ditch some from the back
			while( size < items.size() )
				items.remove( items.size()-1 );
		}
		else if( size > items.size() )
		{
			// we don't have enough space; add some empty elements to push it out
			while( size > items.size() )
				items.add( factory.createElement(0) );
		}
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
			// write the size
			wrapper.putInt( items.size() );
			// write the items
			for( DataElement item : this.items )
				item.encode( wrapper );
		}
		catch( Exception e )
		{
			throw new EncoderException( "Failed in HLAvariableArray::encode >> "+e.getMessage(), e );
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
			// read the size and verify that we have space
			int count = wrapper.getInt();
			wrapper.verify( count );
			
			// resize the array if necessary
			resize( count );
			
			// decode the values into the array
			for( int i = 0; i < count; i++ )
				items.get(i).decode( wrapper );
		}
		catch( Exception e )
		{
			throw new DecoderException( "Failed in HLAvariableArray::decode >> "+e.getMessage(), e );
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
			// If there are no items, create one and calculate the boundary off that
			if( items.isEmpty() )
			{
				this.boundary = Math.max( 4, factory.createElement(0).getOctetBoundary() );
			}
			else
			{
				// If there are items, get the largest boundary from what they provide.
				// Minimum size is what we use to store the array size.
				int temp = 4;
				for( DataElement item : this.items )
					temp = Math.max( temp, item.getOctetBoundary() );
				
				this.boundary = temp;
			}
		}
		
		return this.boundary;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
