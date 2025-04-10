from decimal import Decimal, getcontext

getcontext().prec = 20

def is_rate_valid(incoming_bid, incoming_ask, cached_bids, cached_asks):

    avg_cached_bid_str = average_of_list(cached_bids)
    avg_cached_ask_str = average_of_list(cached_asks)

    ref_bid = Decimal(avg_cached_bid_str)
    ref_ask = Decimal(avg_cached_ask_str)
    reference_mid = (ref_bid + ref_ask) / 2


    new_bid = Decimal(incoming_bid)
    new_ask = Decimal(incoming_ask)
    mid_of_incoming_rate = (new_bid + new_ask) / 2

    diff = abs(mid_of_incoming_rate - reference_mid) / reference_mid

    return diff <= Decimal("0.01")



def average_of_list(string_list):
    total = Decimal(0)
    for item_str in string_list:
        total += Decimal(item_str)

    avg = total / Decimal(len(string_list))
    return str(avg)