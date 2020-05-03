package org.shield.trade;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class TradeContractTest {

    private SimpleDateFormat dateFormatter;
    private TradeContract tradeContract;

    @Before
    public void setUp() {
        dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        tradeContract = new TradeContract();
    }

    @Test
    public void isDateValid_WithSameDaySettleDate_ShouldBeTrue() throws ParseException {
        Date now = dateFormatter.parse("02-05-2020 15:20");
        Date sameDaySettleDate = dateFormatter.parse("02-05-2020 15:21");

        boolean isDateValid = tradeContract.isSettleDateValid(now, sameDaySettleDate);

        assertTrue(isDateValid);
    }

    @Test
    public void isDateValid_WithFutureDaySettleDate_ShouldBeTrue() throws ParseException {
        Date now = dateFormatter.parse("02-05-2020 15:20");
        Date inTheFutureSettleDate = dateFormatter.parse("03-05-2020 15:21");

        boolean isDateValid = tradeContract.isSettleDateValid(now, inTheFutureSettleDate);

        assertTrue(isDateValid);
    }

    @Test
    public void isDateValid_WithDayInThePastSettleDate_ShouldBeFalse() throws ParseException {
        Date now = dateFormatter.parse("02-05-2020 15:20");
        Date inThePastSettleDate = dateFormatter.parse("01-05-2020 15:21");

        boolean isDateValid = tradeContract.isSettleDateValid(now, inThePastSettleDate);

        assertFalse(isDateValid);
    }
}