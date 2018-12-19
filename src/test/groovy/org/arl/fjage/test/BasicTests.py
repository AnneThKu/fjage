import unittest
from fjagepy import *


class MyTestCase(unittest.TestCase):

    global g
    g = Gateway('localhost', 5081, "PythonGW")
    if g is None:
        print("Could not connect to fjage master container on localhost:5081")

    def test_gateway_connection(self):
        """TEST"""
        print('shortDescription():', self.shortDescription())
        self.assertIsInstance(g, Gateway)

    def test_gateway_agentid(self):
        self.assertEqual(g.getAgentID(), "PythonGW")

    def test_subscribe_unsubscribe_topic(self):
        g.subscribe(g.topic("abc"))
        self.assertIn("abc", g.subscribers)
        g.subscribe(g.topic("def"))
        self.assertIn("def", g.subscribers)
        g.unsubscribe(g.topic("abc"))
        self.assertNotIn("abc", g.subscribers)
        g.subscribe(g.topic("ghi"))
        self.assertIn("ghi", g.subscribers)
        g.unsubscribe(g.topic("def"))
        self.assertNotIn("def", g.subscribers)

    def test_subscribe_unsubscribe_agent(self):
        g.subscribe(AgentID(g, "abc"))
        self.assertIn("abc__ntf", g.subscribers)
        g.subscribe(AgentID(g, "def", True))
        self.assertIn("def", g.subscribers)
        g.unsubscribe(AgentID(g, "abc"))
        self.assertNotIn("abc__ntf", g.subscribers)
        g.unsubscribe(AgentID(g, "def", True))
        self.assertNotIn("def", g.subscribers)

    def test_send_Message(self):
        m = Message(recipient='test')
        self.assertIsInstance(m, Message)
        self.assertTrue(g.send(m))


if __name__ == "__main__":
    unittest.main()
