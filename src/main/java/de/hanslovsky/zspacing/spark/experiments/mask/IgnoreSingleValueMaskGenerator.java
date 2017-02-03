package de.hanslovsky.zspacing.spark.experiments.mask;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

public class IgnoreSingleValueMaskGenerator< I extends IntegerType< I >, R extends RealType< R > > implements MaskGenerator< I, R >
{

	private final I ignoreValue;

	public IgnoreSingleValueMaskGenerator( final I ignoreValue )
	{
		super();
		this.ignoreValue = ignoreValue;
	}

	@Override
	public void generateMask( final RandomAccessible< I > image, final RandomAccessibleInterval< R > mask )
	{
		for ( final Pair< I, R > p : Views.interval( Views.pair( image, mask ), mask ) )
			p.getB().setReal( p.getA().valueEquals( ignoreValue ) ? 0.0 : 1.0 );
	}

}
