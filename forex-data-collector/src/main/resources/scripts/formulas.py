from decimal import Decimal, getcontext

getcontext().prec = 20

def is_rate_valid(incoming_bid, incoming_ask, cached_bids, cached_asks):

    avg_of_cached_bids_str = calculate_average_of_bid_or_ask_list(cached_bids)
    avg_of_cached_asks_str = calculate_average_of_bid_or_ask_list(cached_asks)

    ref_bid = Decimal(avg_of_cached_bids_str)
    ref_ask = Decimal(avg_of_cached_asks_str)
    reference_mid = (ref_bid + ref_ask) / 2


    new_bid = Decimal(incoming_bid)
    new_ask = Decimal(incoming_ask)
    mid_of_incoming_rate = (new_bid + new_ask) / 2

    diff = abs(mid_of_incoming_rate - reference_mid) / reference_mid * 100

    return diff <= Decimal("1")





def calculate_usd_try(cached_bids, cached_asks):
    avg_of_cached_bids_str = calculate_average_of_bid_or_ask_list(cached_bids)
    avg_of_cached_asks_str = calculate_average_of_bid_or_ask_list(cached_asks)

    return [avg_of_cached_bids_str, avg_of_cached_asks_str]










def calculate_average_of_bid_or_ask_list(string_list):
    total = Decimal(0)
    for item_str in string_list:
        total += Decimal(item_str)

    avg = total / Decimal(len(string_list))
    return str(avg)