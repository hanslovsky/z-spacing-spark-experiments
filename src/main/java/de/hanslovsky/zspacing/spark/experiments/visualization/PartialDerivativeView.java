package de.hanslovsky.zspacing.spark.experiments.visualization;

import net.imglib2.AbstractInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.View;
import net.imglib2.type.Type;
import net.imglib2.type.operators.Sub;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class PartialDerivativeView< T extends Sub< T > & Type< T > > extends AbstractInterval implements RandomAccessibleInterval< T >, View
{

	private final RandomAccessible< T > source;

	private final int d;

	private final RandomAccessible< T > forward;

	private final RandomAccessible< T > backward;

	private final T t;

	public PartialDerivativeView( final RandomAccessible< T > source, final Interval interval, final int d )
	{
		super( interval );
		this.source = source;
		this.d = d;
		final long[] forwardOffset = new long[ source.numDimensions() ];
		final long[] backwardOffset = new long[ source.numDimensions() ];
		forwardOffset[ this.d ] = 1;
		backwardOffset[ this.d ] = -1;
		this.forward = Views.translate( this.source, forwardOffset );
		this.backward = Views.translate( this.source, backwardOffset );
		this.t = Util.getTypeFromInterval( Views.interval( source, interval ) );
	}

	@Override
	public PartialDerivativeAccess< T > randomAccess()
	{
		return new PartialDerivativeAccess<>( t.createVariable(), forward.randomAccess(), backward.randomAccess() );
	}

	@Override
	public PartialDerivativeAccess< T > randomAccess( final Interval interval )
	{
		return randomAccess();
	}

	public static class PartialDerivativeAccess< T extends Sub< T > & Type< T > > extends Point implements RandomAccess< T >
	{

		private final T t;

		private final RandomAccess< T > forward;

		private final RandomAccess< T > backward;

		public PartialDerivativeAccess( final T t, final RandomAccess< T > forward, final RandomAccess< T > backward )
		{
			super( forward.numDimensions() );
			this.t = t;
			this.forward = forward;
			this.backward = backward;
		}

		@Override
		public T get()
		{
			forward.setPosition( this );
			backward.setPosition( this );
			t.set( forward.get() );
			t.sub( backward.get() );
			return t;
		}

		@Override
		public PartialDerivativeAccess< T > copy()
		{
			return copyRandomAccess();
		}

		@Override
		public PartialDerivativeAccess< T > copyRandomAccess()
		{
			final PartialDerivativeAccess< T > other = new PartialDerivativeAccess<>( t.createVariable(), forward.copyRandomAccess(), backward.copyRandomAccess() );
			other.setPosition( this );
			return other;
		}

	}

}
