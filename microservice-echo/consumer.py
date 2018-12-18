import os
import json

from kombu import Connection, Exchange, Queue, Producer, Consumer
from kombu.asynchronous import Hub


MICROSERVICE_BENCHMARK = "api.matchmaking.benchmark"

EXCHANGE_REQUEST = Exchange("open-matchmaking.direct", type="direct")
EXCHANGE_RESPONSE = Exchange("open-matchmaking.responses.direct", type="direct")
QUEUE_REQUEST = Queue(MICROSERVICE_BENCHMARK, EXCHANGE_REQUEST, MICROSERVICE_BENCHMARK)


def get_connection_url():
    return "{}://{}:{}@{}:{}/{}".format(
        "amqps" if os.getenv("AMQP_SSL", False) else "amqp",
        os.getenv("AMQP_USERNAME", "user"),
        os.getenv("AMQP_PASSWORD", "password"),
        os.getenv("AMQP_HOST", "rabbitmq"),
        os.getenv("AMQP_PORT", 5672),
        os.getenv("AMQP_VIRTUAL_HOST", "vhost"),
    )


class ConsumerBenchmark(Consumer):

    def __init__(self, *args, **kwargs):
        super(ConsumerBenchmark, self).__init__(*args, **kwargs)
        self.on_message = self.process_request
        self.prefetch_count = 50

    def process_request(self, message):
        response = {
            "content": json.loads(message.body) or {},
            "event-name": message.properties.get('correlation_id', None)
        }

        reply_to = message.properties['reply_to']
        producer = Producer(self.connection)
        producer.publish(
            json.dumps(response),
            exchange=EXCHANGE_RESPONSE,
            routing_key=reply_to,
            headers=message.headers,
            retry=True,
        )
        producer.close()

        message.ack()


def run_microservice():
    hub = Hub()
    url = get_connection_url()
    connection = Connection(url)
    connection.register_with_event_loop(hub)

    EXCHANGE_REQUEST.declare(channel=connection.channel())
    EXCHANGE_RESPONSE.declare(channel=connection.channel())

    with ConsumerBenchmark(connection, [QUEUE_REQUEST]):
        hub.run_forever()


if __name__ == '__main__':
   run_microservice()
