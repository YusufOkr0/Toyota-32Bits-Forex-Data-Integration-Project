from decimal import Decimal, getcontext

getcontext().prec = 20

def is_rate_valid(incoming_bid_str, incoming_ask_str, cached_bids, cached_asks):

    avg_of_cached_bids_dec = calculate_average_of_bid_or_ask_list(cached_bids)
    avg_of_cached_asks_dec = calculate_average_of_bid_or_ask_list(cached_asks)

    reference_mid = (avg_of_cached_bids_dec + avg_of_cached_asks_dec) / 2

    new_bid = Decimal(incoming_bid_str)
    new_ask = Decimal(incoming_ask_str)

    mid_of_incoming_rate = (new_bid + new_ask) / 2

    diff = abs(mid_of_incoming_rate - reference_mid) / reference_mid * 100

    return diff <= Decimal("1")





def calculate_usd_try(cached_bids, cached_asks):
    avg_of_cached_bids_dec = calculate_average_of_bid_or_ask_list(cached_bids)
    avg_of_cached_asks_dec = calculate_average_of_bid_or_ask_list(cached_asks)

    return [str(avg_of_cached_bids_dec), str(avg_of_cached_asks_dec)]





def calculate_usd_try_mid_value(cached_usd_try_bids, cached_usd_try_asks):
    avg_of_usd_try_bids_dec = calculate_average_of_bid_or_ask_list(cached_usd_try_bids)
    avg_of_usd_try_asks_dec = calculate_average_of_bid_or_ask_list(cached_usd_try_asks)

    usd_try_mid_dec = (avg_of_usd_try_bids_dec + avg_of_usd_try_asks_dec) / 2

    return str(usd_try_mid_dec)











def calculate_average_of_bid_or_ask_list(string_list):
    total = Decimal(0)
    for item_str in string_list:
        total += Decimal(item_str)

    avg = total / Decimal(len(string_list))
    return avg