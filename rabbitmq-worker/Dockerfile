FROM python:3.6.3-slim-jessie

RUN mkdir /code
COPY rabbitmq-worker/ /code
WORKDIR /code

RUN pip3 install -r requirements.txt
CMD ["python3", "consumer.py"]
